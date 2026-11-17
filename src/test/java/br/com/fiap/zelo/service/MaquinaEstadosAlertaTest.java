package br.com.fiap.zelo.service;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.enums.Comodo;
import br.com.fiap.zelo.enums.DesfechoAlerta;
import br.com.fiap.zelo.enums.EstadoAlerta;
import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.enums.EstadoSensor;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.OrigemAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import br.com.fiap.zelo.enums.TipoSensor;
import br.com.fiap.zelo.model.Alerta;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.EtapaAlerta;
import br.com.fiap.zelo.repository.AlertaRepository;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.repository.ConversaCheckinRepository;
import br.com.fiap.zelo.repository.EtapaAlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class MaquinaEstadosAlertaTest {

    private static final UUID CASA_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

    @Autowired
    private MaquinaEstadosAlertaService maquinaEstadosService;

    @Autowired
    private MotorRegrasService motorRegrasService;

    @Autowired
    private CasaMonitoradaRepository casaRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private EtapaAlertaRepository etapaAlertaRepository;

    @Autowired
    private ConversaCheckinRepository conversaCheckinRepository;

    @BeforeEach
    void setup() {
        conversaCheckinRepository.deleteAll();
        etapaAlertaRepository.deleteAll();
        alertaRepository.deleteAll();

        CasaMonitorada casa = casaRepository.findById(CASA_ID).orElseThrow();
        casa.setEstadoCasa(EstadoCasa.EM_CASA);
        casa.setPortaEntradaAbertaEm(null);
        casaRepository.save(casa);
    }

    @Test
    @DisplayName("Ciclo completo do Roteiro 3: AGUARDANDO_IDOSO -> Nível 1 Ana -> Nível 2 José -> Confirmação -> Desfecho")
    void cicloCompleto_escalada_confirmacao_desfecho() {
        Instant agora = Instant.parse("2026-11-17T08:55:00Z");

        // 1. Abre alerta R2 CRITICO -> AGUARDANDO_IDOSO (espera de 3 min)
        Alerta alerta = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.R2,
                GravidadeAlerta.CRITICO,
                OrigemAlerta.SENSOR,
                "Possível queda no banheiro",
                agora
        );

        assertEquals(EstadoAlerta.AGUARDANDO_IDOSO, alerta.getEstado());
        assertEquals(0, alerta.getNivelEscaladaAtual());
        assertNotNull(alerta.getProximaAcaoEm());
        assertEquals(agora.plus(Duration.ofMinutes(3)), alerta.getProximaAcaoEm());

        // 2. Timeout de 3 min sem resposta do idoso -> escala para Nível 1 (Ana)
        Instant tTimeoutIdoso = agora.plus(Duration.ofMinutes(3));
        maquinaEstadosService.verificarTimeouts(CASA_ID, tTimeoutIdoso);

        Alerta escaladoNivel1 = alertaRepository.findById(alerta.getId()).orElseThrow();
        assertEquals(EstadoAlerta.ESCALADO, escaladoNivel1.getEstado());
        assertEquals(1, escaladoNivel1.getNivelEscaladaAtual());
        assertEquals(1, escaladoNivel1.getEtapas().size());

        EtapaAlerta etapa1 = escaladoNivel1.getEtapas().get(0);
        assertEquals(1, etapa1.getNivel());
        assertEquals("Ana (Filha)", etapa1.getNomeContato());
        assertEquals("WHATSAPP", etapa1.getCanal());

        // 3. Timeout de 5 min do nível 1 sem resposta -> avança para Nível 2 (Seu José)
        Instant tTimeoutAna = escaladoNivel1.getProximaAcaoEm();
        maquinaEstadosService.verificarTimeouts(CASA_ID, tTimeoutAna);

        Alerta escaladoNivel2 = alertaRepository.findById(alerta.getId()).orElseThrow();
        assertEquals(EstadoAlerta.ESCALADO, escaladoNivel2.getEstado());
        assertEquals(2, escaladoNivel2.getNivelEscaladaAtual());
        assertEquals(2, escaladoNivel2.getEtapas().size());

        EtapaAlerta etapa2 = escaladoNivel2.getEtapas().get(1);
        assertEquals(2, etapa2.getNivel());
        assertEquals("Seu José (Vizinho)", etapa2.getNomeContato());

        // 4. Cuidador confirma recebimento ("Vou verificar") -> transiciona para EM_VERIFICACAO
        Instant tConfirmacao = tTimeoutAna.plusSeconds(30);
        Alerta emVerificacao = maquinaEstadosService.confirmarAlerta(
                alerta.getId(),
                etapa2.getContatoId(),
                etapa2.getNomeContato(),
                tConfirmacao
        );

        assertEquals(EstadoAlerta.EM_VERIFICACAO, emVerificacao.getEstado());
        assertNull(emVerificacao.getProximaAcaoEm());

        // 5. Cuidador conclui atendimento e registra desfecho -> ENCERRADO
        Instant tDesfecho = tConfirmacao.plus(Duration.ofMinutes(15));
        Alerta encerrado = maquinaEstadosService.registrarDesfecho(
                alerta.getId(),
                DesfechoAlerta.PRECISOU_AJUDA,
                "Dona Maria escorregou no tapete, mas já está bem e acomodada no quarto.",
                tDesfecho
        );

        assertEquals(EstadoAlerta.ENCERRADO, encerrado.getEstado());
        assertEquals(DesfechoAlerta.PRECISOU_AJUDA, encerrado.getDesfecho());
        assertEquals(tDesfecho, encerrado.getEncerradoEm());
    }

    @Test
    @DisplayName("Conflito de E1 (Emergência) durante alerta aberto promove alerta para E1 CRITICO e escala imediatamente")
    void conflitoE1_promoveAlertaAberto() {
        Instant agora = Instant.parse("2026-11-17T08:50:00Z");

        // Alerta R1 ATENCAO aberto aguardando idoso
        Alerta alerta = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.R1,
                GravidadeAlerta.ATENCAO,
                OrigemAlerta.SENSOR,
                "Sem atividade matinal",
                agora
        );
        assertEquals(RegraTipo.R1, alerta.getRegra());
        assertEquals(EstadoAlerta.AGUARDANDO_IDOSO, alerta.getEstado());

        // Usuário aciona botão de Emergência E1
        Instant tEmergencia = agora.plus(Duration.ofMinutes(2));
        Alerta promovido = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.E1,
                GravidadeAlerta.CRITICO,
                OrigemAlerta.EMERGENCIA,
                "Botão físico de emergência acionado",
                tEmergencia
        );

        assertEquals(alerta.getId(), promovido.getId(), "Deve manter o mesmo alerta");
        assertEquals(RegraTipo.E1, promovido.getRegra());
        assertEquals(GravidadeAlerta.CRITICO, promovido.getGravidade());
        assertEquals(OrigemAlerta.EMERGENCIA, promovido.getOrigem());
        assertEquals(EstadoAlerta.ESCALADO, promovido.getEstado(), "E1 pula check-in e vai direto para ESCALADO");
        assertEquals(1, promovido.getNivelEscaladaAtual());
    }

    @Test
    @DisplayName("Conflito de R5 (Lembrete não confirmado) durante alerta aberto apenas concatena contexto")
    void conflitoR5_concatenaContextoSemDuplicar() {
        Instant agora = Instant.parse("2026-11-17T08:55:00Z");

        Alerta alertaOriginal = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.R2,
                GravidadeAlerta.CRITICO,
                OrigemAlerta.SENSOR,
                "Tempo excessivo no banheiro",
                agora
        );

        // Disparo de R5
        Instant tLembrete = agora.plus(Duration.ofMinutes(1));
        Alerta atualizado = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.R5,
                GravidadeAlerta.ATENCAO,
                OrigemAlerta.LEMBRETE,
                "Lembrete de Losartana não confirmado",
                tLembrete
        );

        assertEquals(alertaOriginal.getId(), atualizado.getId());
        assertEquals(RegraTipo.R2, atualizado.getRegra(), "Regra mais crítica original é mantida");
        assertTrue(atualizado.getContexto().contains("Tempo excessivo no banheiro"));
        assertTrue(atualizado.getContexto().contains("Lembrete de Losartana não confirmado"));
        assertEquals(1, alertaRepository.count());
    }

    @Test
    @DisplayName("Novo evento de sensor durante alerta aberto NÃO fecha o alerta")
    void novoEventoSensorDuranteAlertaAberto_naoFechaAlerta() {
        Instant agora = Instant.parse("2026-11-17T08:55:00Z");

        Alerta alerta = maquinaEstadosService.abrirAlerta(
                CASA_ID,
                RegraTipo.R2,
                GravidadeAlerta.CRITICO,
                OrigemAlerta.SENSOR,
                "Possível queda no banheiro",
                agora
        );

        // Chega novo evento de sensor na cozinha
        Instant tNovoEvento = agora.plus(Duration.ofMinutes(1));
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.COZINHA, EstadoSensor.ATIVO, tNovoEvento, true));

        Alerta alertaAposSensor = alertaRepository.findById(alerta.getId()).orElseThrow();
        assertEquals(EstadoAlerta.AGUARDANDO_IDOSO, alertaAposSensor.getEstado(), "Alerta não pode ser encerrado por sensor automático");
        assertNull(alertaAposSensor.getEncerradoEm());

        CasaMonitorada casa = casaRepository.findById(CASA_ID).orElseThrow();
        assertEquals("COZINHA", casa.getUltimoComodo(), "Estado da casa é atualizado");
    }
}
