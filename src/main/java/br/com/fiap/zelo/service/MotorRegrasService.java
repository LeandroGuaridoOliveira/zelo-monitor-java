package br.com.fiap.zelo.service;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.enums.Comodo;
import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.enums.EstadoSensor;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.RegraConfiguracao;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.repository.RegraConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorRegrasService {

    public static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private final CasaMonitoradaRepository casaRepository;
    private final RegraConfiguracaoRepository regraConfiguracaoRepository;
    private final RelogioService relogioService;
    private final IntegracaoExternaService integracaoExternaService;
    private final MaquinaEstadosAlertaService maquinaEstadosService;

    @Transactional
    public void processarEvento(EventoSensorMessageDto evento) {
        if (evento == null || evento.casaId() == null) return;

        CasaMonitorada casa = casaRepository.findById(evento.casaId()).orElse(null);
        if (casa == null) {
            log.warn("Evento de sensor ignorado: casa {} não encontrada", evento.casaId());
            return;
        }

        Instant agora = evento.ocorridoEm() != null ? evento.ocorridoEm() : relogioService.obterAgora(casa.getId());
        relogioService.atualizarRelogio(casa.getId(), agora);

        // Lógica de porta de entrada e presença interna
        if (evento.comodo() == Comodo.ENTRADA) {
            if (evento.estado() == EstadoSensor.ABERTA) {
                casa.setPortaEntradaAbertaEm(agora);
            }
        } else {
            // Qualquer evento em cômodo interno (QUARTO, BANHEIRO, COZINHA, GELADEIRA) cancela a saída
            casa.setPortaEntradaAbertaEm(null);
            casa.setEstadoCasa(EstadoCasa.EM_CASA);
        }

        casa.setUltimoComodo(evento.comodo().name());
        casa.setUltimoEstadoSensor(evento.estado().name());
        casa.setUltimoEventoEm(agora);
        casa = casaRepository.save(casa);

        // Verifica timeouts de alertas já existentes e avalia regras
        maquinaEstadosService.verificarTimeouts(casa.getId(), agora);
        avaliarRegras(casa, agora);
    }

    @Transactional
    public void processarAvancoRelogio(UUID casaId, Instant agora) {
        if (casaId == null || agora == null) return;

        relogioService.atualizarRelogio(casaId, agora);

        CasaMonitorada casa = casaRepository.findById(casaId).orElse(null);
        if (casa == null) return;

        // Modo Fora de Casa: Se porta de entrada foi aberta há >= 10 minutos sem eventos internos
        if (casa.getPortaEntradaAbertaEm() != null) {
            long minutosAberta = Duration.between(casa.getPortaEntradaAbertaEm(), agora).toMinutes();
            if (minutosAberta >= 10) {
                if (casa.getEstadoCasa() != EstadoCasa.FORA_DE_CASA) {
                    log.info("Casa {}: porta de entrada aberta há {} minutos sem eventos internos. Mudando para FORA_DE_CASA.",
                            casaId, minutosAberta);
                    casa.setEstadoCasa(EstadoCasa.FORA_DE_CASA);
                    casa = casaRepository.save(casa);
                }
            }
        }

        // Verifica timeouts da máquina de estados (ex.: aguardando resposta do idoso ou cuidador)
        maquinaEstadosService.verificarTimeouts(casaId, agora);

        // Avalia regras R1, R2, R4
        avaliarRegras(casa, agora);
    }

    public void avaliarRegras(CasaMonitorada casa, Instant agora) {
        // Regras de sensor dependem de consentimento ativo e estado EM_CASA
        if (!Boolean.TRUE.equals(casa.getConsentimentoAtivo())) {
            log.debug("Casa {}: monitoramento suspenso (consentimento desativado).", casa.getId());
            return;
        }

        if (casa.getEstadoCasa() != EstadoCasa.EM_CASA) {
            log.debug("Casa {}: monitoramento suspenso (morador FORA_DE_CASA).", casa.getId());
            return;
        }

        // Se já existe alerta aberto, não abre duplicatas de regras de sensor
        if (maquinaEstadosService.buscarAlertaAberto(casa.getId()).isPresent()) {
            return;
        }

        RegraConfiguracao config = regraConfiguracaoRepository.findByCasaId(casa.getId())
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

        ZonedDateTime agoraSp = agora.atZone(ZONE_SP);
        LocalTime horaAtual = agoraSp.toLocalTime();
        LocalDate dataAtual = agoraSp.toLocalDate();

        // 1. R2 - Banheiro prolongado (CRITICO)
        // Último evento foi BANHEIRO ATIVO e tempo transcorrido >= limiteBanheiroMin (40 min)
        if ("BANHEIRO".equalsIgnoreCase(casa.getUltimoComodo())
                && "ATIVO".equalsIgnoreCase(casa.getUltimoEstadoSensor())
                && casa.getUltimoEventoEm() != null) {
            long minutosBanheiro = Duration.between(casa.getUltimoEventoEm(), agora).toMinutes();
            if (minutosBanheiro >= config.getLimiteBanheiroMin()) {
                log.warn("Disparo R2 na casa {}: {} min no banheiro (limite: {})",
                        casa.getId(), minutosBanheiro, config.getLimiteBanheiroMin());
                maquinaEstadosService.abrirAlerta(
                        casa.getId(),
                        RegraTipo.R2,
                        GravidadeAlerta.CRITICO,
                        OrigemAlerta.SENSOR,
                        "Tempo excessivo no banheiro: " + minutosBanheiro + " minutos sem nova movimentação",
                        agora
                );
                return;
            }
        }

        // Verifica se houve evento interno hoje após as 04:00
        boolean teveEventoHoje = false;
        if (casa.getUltimoEventoEm() != null) {
            ZonedDateTime ultimoEventoSp = casa.getUltimoEventoEm().atZone(ZONE_SP);
            if (ultimoEventoSp.toLocalDate().isEqual(dataAtual)
                    && !ultimoEventoSp.toLocalTime().isBefore(LocalTime.of(4, 0))
                    && !"ENTRADA".equalsIgnoreCase(casa.getUltimoComodo())) {
                teveEventoHoje = true;
            }
        }

        // 2. R1 - Sem atividade matinal (ATENCAO)
        // No dia atual, entre 04:00 e 12:00, sem evento interno hoje, e horaAtual >= horarioHabitual + toleranciaMatinal
        if (!horaAtual.isBefore(LocalTime.of(4, 0)) && horaAtual.isBefore(LocalTime.of(12, 0))) {
            if (!teveEventoHoje) {
                IntegracaoExternaService.PerfilRotinaDto perfil = integracaoExternaService.buscarPerfilRotina(casa.getId());
                String horarioBase = (perfil != null && perfil.horarioHabitualLevantar() != null)
                        ? perfil.horarioHabitualLevantar()
                        : config.getHorarioHabitualLevantar();

                LocalTime base = LocalTime.parse(horarioBase);
                LocalTime limiteMatinal = base.plusMinutes(config.getToleranciaMatinalMin());

                if (!horaAtual.isBefore(limiteMatinal)) {
                    log.warn("Disparo R1 na casa {}: sem atividade matinal às {} (limite {})",
                            casa.getId(), horaAtual, limiteMatinal);
                    maquinaEstadosService.abrirAlerta(
                            casa.getId(),
                            RegraTipo.R1,
                            GravidadeAlerta.ATENCAO,
                            OrigemAlerta.SENSOR,
                            "Sem atividade matinal detectada após o horário previsto (" + limiteMatinal + ")",
                            agora
                    );
                    return;
                }
            }
        }

        // 3. R4 - Inatividade prolongada (ATENCAO)
        // Entre 07:00 e 22:00, tempo sem evento >= limiteInatividadeMin (240 min = 4h).
        // Se a pessoa ainda não acordou (antes das 12:00 sem evento hoje), R1 é quem governa a ausência de atividade matinal.
        if (teveEventoHoje || !horaAtual.isBefore(LocalTime.of(12, 0))) {
            if (!horaAtual.isBefore(LocalTime.of(7, 0)) && horaAtual.isBefore(LocalTime.of(22, 0))) {
                if (casa.getUltimoEventoEm() != null) {
                    long minutosInativo = Duration.between(casa.getUltimoEventoEm(), agora).toMinutes();
                    if (minutosInativo >= config.getLimiteInatividadeMin()) {
                        log.warn("Disparo R4 na casa {}: inatividade de {} min (limite {})",
                                casa.getId(), minutosInativo, config.getLimiteInatividadeMin());
                        maquinaEstadosService.abrirAlerta(
                                casa.getId(),
                                RegraTipo.R4,
                                GravidadeAlerta.ATENCAO,
                                OrigemAlerta.SENSOR,
                                "Inatividade prolongada detectada: " + minutosInativo + " minutos sem eventos",
                                agora
                        );
                    }
                }
            }
        }
    }
}
