package br.com.fiap.zelo.service;

import br.com.fiap.zelo.enums.ClassificacaoCheckin;
import br.com.fiap.zelo.enums.DesfechoAlerta;
import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import br.com.fiap.zelo.model.Alerta;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.EtapaAlerta;
import br.com.fiap.zelo.model.RegraConfiguracao;
import br.com.fiap.zelo.repository.AlertaRepository;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.repository.RegraConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaquinaEstadosAlertaService {

    public static final Set<EstadoAlerta> ESTADOS_ABERTOS = Set.of(
            EstadoAlerta.AGUARDANDO_IDOSO,
            EstadoAlerta.ESCALADO,
            EstadoAlerta.EM_VERIFICACAO
    );

    private final AlertaRepository alertaRepository;
    private final CasaMonitoradaRepository casaRepository;
    private final RegraConfiguracaoRepository regraConfiguracaoRepository;
    private final IntegracaoExternaService integracaoExternaService;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<Alerta> buscarAlertaAberto(UUID casaId) {
        return alertaRepository.findFirstByCasaIdAndEstadoInOrderByAbertoEmDesc(casaId, ESTADOS_ABERTOS);
    }

    @Transactional(readOnly = true)
    public Alerta buscarAlerta(UUID alertaId) {
        return alertaRepository.findById(alertaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta não encontrado: " + alertaId));
    }

    @Transactional
    public Alerta abrirAlerta(UUID casaId, RegraTipo regra, GravidadeAlerta gravidade, OrigemAlerta origem, String contexto, Instant agora) {
        Optional<Alerta> abertoOpt = buscarAlertaAberto(casaId);

        if (abertoOpt.isPresent()) {
            Alerta existente = abertoOpt.get();
            if (regra == RegraTipo.E1) {
                log.info("Alerta {} na casa {} promovido para emergência E1", existente.getId(), casaId);
                existente.setGravidade(GravidadeAlerta.CRITICO);
                existente.setRegra(RegraTipo.E1);
                existente.setOrigem(OrigemAlerta.EMERGENCIA);
                existente.setContexto((existente.getContexto() != null ? existente.getContexto() + " | " : "") + contexto);
                escalarAlerta(existente, 1, agora);
                return alertaRepository.save(existente);
            } else {
                log.info("Novo evento da regra {} recebido para casa {}, concatenando ao alerta existente {}", regra, casaId, existente.getId());
                existente.setContexto((existente.getContexto() != null ? existente.getContexto() + " | " : "") + contexto);
                return alertaRepository.save(existente);
            }
        }

        CasaMonitorada casa = casaRepository.findById(casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Casa não encontrada: " + casaId));

        RegraConfiguracao regraConfig = obterRegraConfig(casaId);

        Alerta alerta = Alerta.builder()
                .id(UUID.randomUUID())
                .casa(casa)
                .regra(regra)
                .gravidade(gravidade)
                .origem(origem)
                .contexto(contexto)
                .abertoEm(agora)
                .nivelEscaladaAtual(0)
                .build();

        if (regra == RegraTipo.E1) {
            log.warn("Emergência E1 acionada para casa {}. Pulando check-in e escalando imediatamente.", casaId);
            escalarAlerta(alerta, 1, agora);
        } else {
            alerta.setEstado(EstadoAlerta.AGUARDANDO_IDOSO);
            int tempoEspera = (gravidade == GravidadeAlerta.CRITICO)
                    ? regraConfig.getEsperaCheckinCriticoMin()
                    : regraConfig.getEsperaCheckinAtencaoMin();
            alerta.setProximaAcaoEm(agora.plus(Duration.ofMinutes(tempoEspera)));
            log.info("Alerta {} aberto para casa {} na regra {} ({}), aguardando idoso até {}",
                    alerta.getId(), casaId, regra, gravidade, alerta.getProximaAcaoEm());
        }

        return alertaRepository.save(alerta);
    }

    @Transactional
    public Alerta processarRespostaCheckin(UUID alertaId, ClassificacaoCheckin classificacao, Instant agora) {
        Alerta alerta = buscarAlerta(alertaId);

        if (alerta.getEstado() != EstadoAlerta.AGUARDANDO_IDOSO) {
            log.info("Alerta {} não está em AGUARDANDO_IDOSO (estado atual: {}), ignorando resposta de checkin.", alertaId, alerta.getEstado());
            return alerta;
        }

        if (classificacao == ClassificacaoCheckin.ESTOU_BEM) {
            log.info("Check-in do idoso recebido como ESTOU_BEM para alerta {}. Resolvido!", alertaId);
            alerta.setEstado(EstadoAlerta.RESOLVIDO_PELO_IDOSO);
            alerta.setEncerradoEm(agora);
            alerta.setProximaAcaoEm(null);
            alerta.setDesfecho(DesfechoAlerta.ESTAVA_BEM);
        } else {
            log.warn("Check-in do idoso para alerta {} classificado como {}. Escalando para nível 1!", alertaId, classificacao);
            escalarAlerta(alerta, 1, agora);
        }

        return alertaRepository.save(alerta);
    }

    @Transactional
    public void escalarAlerta(Alerta alerta, int nivel, Instant agora) {
        alerta.setEstado(EstadoAlerta.ESCALADO);
        alerta.setNivelEscaladaAtual(nivel);

        IntegracaoExternaService.RedeApoioDto rede = integracaoExternaService.buscarRedeApoio(alerta.getCasa().getId());
        Optional<IntegracaoExternaService.ContatoApoio> contatoOpt = rede.contatos().stream()
                .filter(c -> c.ordem() != null && c.ordem() == nivel)
                .findFirst();

        if (contatoOpt.isPresent()) {
            IntegracaoExternaService.ContatoApoio contato = contatoOpt.get();
            RegraConfiguracao regraConfig = obterRegraConfig(alerta.getCasa().getId());
            int tempoEspera = (alerta.getGravidade() == GravidadeAlerta.CRITICO)
                    ? regraConfig.getEsperaEscaladaCriticoMin()
                    : regraConfig.getEsperaEscaladaAtencaoMin();

            alerta.setProximaAcaoEm(agora.plus(Duration.ofMinutes(tempoEspera)));

            EtapaAlerta etapa = EtapaAlerta.builder()
                    .id(UUID.randomUUID())
                    .alerta(alerta)
                    .nivel(nivel)
                    .contatoId(contato.contatoId())
                    .nomeContato(contato.nome())
                    .canal(contato.canal() != null ? contato.canal() : "WHATSAPP")
                    .enviadoEm(agora)
                    .build();

            alerta.getEtapas().add(etapa);
            log.info("Alerta {} escalado para nível {} (Contato: {} via {}). Próxima ação em {}",
                    alerta.getId(), nivel, contato.nome(), etapa.getCanal(), alerta.getProximaAcaoEm());
        } else {
            log.warn("Alerta {}: todos os contatos da rede foram acionados sem resposta. Transicionando para SEM_RESPOSTA.", alerta.getId());
            alerta.setEstado(EstadoAlerta.SEM_RESPOSTA);
            alerta.setEncerradoEm(agora);
            alerta.setProximaAcaoEm(null);
        }
    }

    @Transactional
    public Alerta confirmarAlerta(UUID alertaId, UUID contatoId, String nomeContato, Instant agora) {
        Alerta alerta = buscarAlerta(alertaId);

        if (alerta.getEstado() == EstadoAlerta.ESCALADO) {
            alerta.setEstado(EstadoAlerta.EM_VERIFICACAO);
            alerta.setProximaAcaoEm(null);

            if (!alerta.getEtapas().isEmpty()) {
                EtapaAlerta ultimaEtapa = alerta.getEtapas().get(alerta.getEtapas().size() - 1);
                ultimaEtapa.setRespondidoEm(agora);
                ultimaEtapa.setResposta("VOU_VERIFICAR");
                if (contatoId != null) {
                    ultimaEtapa.setContatoId(contatoId);
                }
                if (nomeContato != null) {
                    ultimaEtapa.setNomeContato(nomeContato);
                }
            }
            log.info("Alerta {} confirmado por {}. Estado agora é EM_VERIFICACAO.", alertaId, nomeContato);
        }

        return alertaRepository.save(alerta);
    }

    @Transactional
    public Alerta registrarDesfecho(UUID alertaId, DesfechoAlerta desfecho, String observacao, Instant agora) {
        Alerta alerta = buscarAlerta(alertaId);
        alerta.setEstado(EstadoAlerta.ENCERRADO);
        alerta.setDesfecho(desfecho);
        alerta.setObservacao(observacao);
        alerta.setEncerradoEm(agora);
        alerta.setProximaAcaoEm(null);
        log.info("Alerta {} encerrado com desfecho {}.", alertaId, desfecho);
        return alertaRepository.save(alerta);
    }

    @Transactional
    public void verificarTimeouts(UUID casaId, Instant agora) {
        List<Alerta> alertasAbertos = alertaRepository.findByCasaIdAndEstadoIn(casaId, ESTADOS_ABERTOS);
        for (Alerta alerta : alertasAbertos) {
            if (alerta.getProximaAcaoEm() != null && !agora.isBefore(alerta.getProximaAcaoEm())) {
                if (alerta.getEstado() == EstadoAlerta.AGUARDANDO_IDOSO) {
                    log.info("Timeout de resposta do idoso no alerta {}. Escalando para nível 1.", alerta.getId());
                    escalarAlerta(alerta, 1, agora);
                    alertaRepository.save(alerta);
                } else if (alerta.getEstado() == EstadoAlerta.ESCALADO) {
                    int proximoNivel = alerta.getNivelEscaladaAtual() + 1;
                    log.info("Timeout da etapa atual no alerta {}. Escalando para nível {}.", alerta.getId(), proximoNivel);
                    escalarAlerta(alerta, proximoNivel, agora);
                    alertaRepository.save(alerta);
                }
            }
        }
    }

    private RegraConfiguracao obterRegraConfig(UUID casaId) {
        return regraConfiguracaoRepository.findByCasaId(casaId)
                .orElseGet(() -> RegraConfiguracao.builder()
                        .id(UUID.randomUUID())
                        .horarioHabitualLevantar("07:20")
                        .toleranciaMatinalMin(90)
                        .limiteBanheiroMin(40)
                        .limiteInatividadeMin(240)
                        .esperaCheckinCriticoMin(3)
                        .esperaEscaladaCriticoMin(5)
                        .esperaCheckinAtencaoMin(15)
                        .esperaEscaladaAtencaoMin(15)
                        .build());
    }
}
