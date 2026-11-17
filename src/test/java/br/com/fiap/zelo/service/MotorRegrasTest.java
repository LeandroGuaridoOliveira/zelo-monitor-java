package br.com.fiap.zelo.service;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.enums.Comodo;
import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.enums.EstadoSensor;
import br.com.fiap.zelo.enums.GravidadeAlerta;
import br.com.fiap.zelo.enums.RegraTipo;
import br.com.fiap.zelo.enums.TipoSensor;
import br.com.fiap.zelo.model.Alerta;
import br.com.fiap.zelo.model.CasaMonitorada;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class MotorRegrasTest {

    private static final UUID CASA_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MotorRegrasService motorRegrasService;

    @Autowired
    private MaquinaEstadosAlertaService maquinaEstadosService;

    @Autowired
    private RelogioService relogioService;

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
        casa.setUltimoComodo(null);
        casa.setUltimoEstadoSensor(null);
        casa.setUltimoEventoEm(null);
        casaRepository.save(casa);

        relogioService.resetar(CASA_ID);
    }

    @Test
    @DisplayName("Roteiro 1 (Dia normal): eventos normais não geram alertas")
    void roteiro1_diaNormal_nenhumAlerta() {
        LocalDate hoje = LocalDate.now();

        Instant t0730 = ZonedDateTime.of(hoje, LocalTime.of(7, 30), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.QUARTO, EstadoSensor.ATIVO, t0730, true));

        Instant t0745 = ZonedDateTime.of(hoje, LocalTime.of(7, 45), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.BANHEIRO, EstadoSensor.ATIVO, t0745, true));

        Instant t0800 = ZonedDateTime.of(hoje, LocalTime.of(8, 0), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PORTA, Comodo.BANHEIRO, EstadoSensor.FECHADA, t0800, true));

        Instant t0815 = ZonedDateTime.of(hoje, LocalTime.of(8, 15), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.COZINHA, EstadoSensor.ATIVO, t0815, true));

        assertTrue(maquinaEstadosService.buscarAlertaAberto(CASA_ID).isEmpty());
        CasaMonitorada casa = casaRepository.findById(CASA_ID).orElseThrow();
        assertEquals(EstadoCasa.EM_CASA, casa.getEstadoCasa());
    }

    @Test
    @DisplayName("Roteiro 2 (Ida à feira): saída detectada suspende alertas por 2h e retorno restaura EM_CASA")
    void roteiro2_idaAFeira_foraDeCasa_semAlertas() {
        LocalDate hoje = LocalDate.now();

        // 08:30 - Dona Maria abre a porta de entrada
        Instant t0830 = ZonedDateTime.of(hoje, LocalTime.of(8, 30), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PORTA, Comodo.ENTRADA, EstadoSensor.ABERTA, t0830, true));

        // 08:41 - 11 minutos sem eventos internos -> transiciona para FORA_DE_CASA
        Instant t0841 = ZonedDateTime.of(hoje, LocalTime.of(8, 41), SP).toInstant();
        motorRegrasService.processarAvancoRelogio(CASA_ID, t0841);

        CasaMonitorada casa = casaRepository.findById(CASA_ID).orElseThrow();
        assertEquals(EstadoCasa.FORA_DE_CASA, casa.getEstadoCasa());

        // 10:30 - Quase 2 horas fora: nenhum alerta deve ser disparado
        Instant t1030 = ZonedDateTime.of(hoje, LocalTime.of(10, 30), SP).toInstant();
        motorRegrasService.processarAvancoRelogio(CASA_ID, t1030);
        assertTrue(maquinaEstadosService.buscarAlertaAberto(CASA_ID).isEmpty());

        // 10:45 - Retorno para casa: abre porta e movimenta na cozinha
        Instant t1045 = ZonedDateTime.of(hoje, LocalTime.of(10, 45), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PORTA, Comodo.ENTRADA, EstadoSensor.ABERTA, t1045, true));

        Instant t1046 = ZonedDateTime.of(hoje, LocalTime.of(10, 46), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.COZINHA, EstadoSensor.ATIVO, t1046, true));

        casa = casaRepository.findById(CASA_ID).orElseThrow();
        assertEquals(EstadoCasa.EM_CASA, casa.getEstadoCasa());
        assertTrue(maquinaEstadosService.buscarAlertaAberto(CASA_ID).isEmpty());
    }

    @Test
    @DisplayName("Roteiro 3 (Queda no banheiro - R2): limite exato de 39 min não abre alerta, e 40 min abre R2 CRITICO")
    void roteiro3_quedaNoBanheiro_R2_limiteExato39e40Minutos() {
        LocalDate hoje = LocalDate.now();

        // 08:15 - Entrada no banheiro (ATIVO)
        Instant tEntrada = ZonedDateTime.of(hoje, LocalTime.of(8, 15), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.BANHEIRO, EstadoSensor.ATIVO, tEntrada, true));

        // 08:54 (+39 min) - Limite exato de 39 minutos: NÃO deve abrir alerta
        Instant t39 = tEntrada.plus(Duration.ofMinutes(39));
        motorRegrasService.processarAvancoRelogio(CASA_ID, t39);
        assertTrue(maquinaEstadosService.buscarAlertaAberto(CASA_ID).isEmpty(), "Aos 39 minutos não deve abrir alerta");

        // 08:55 (+40 min) - Limite atingido de 40 minutos: DEVE abrir alerta R2 CRÍTICO
        Instant t40 = tEntrada.plus(Duration.ofMinutes(40));
        motorRegrasService.processarAvancoRelogio(CASA_ID, t40);

        Optional<Alerta> alertaOpt = maquinaEstadosService.buscarAlertaAberto(CASA_ID);
        assertTrue(alertaOpt.isPresent(), "Aos 40 minutos deve abrir alerta R2");

        Alerta alerta = alertaOpt.get();
        assertEquals(RegraTipo.R2, alerta.getRegra());
        assertEquals(GravidadeAlerta.CRITICO, alerta.getGravidade());
    }

    @Test
    @DisplayName("Roteiro 4 (Sem atividade matinal - R1): abre alerta R1 ATENCAO após horarioHabitual + toleranciaMatinal (08:50)")
    void roteiro4_semAtividadeMatinal_R1_abreAlertaAposTolerancia() {
        LocalDate hoje = LocalDate.now();

        // Noite anterior às 22:00
        Instant tOntem = ZonedDateTime.of(hoje.minusDays(1), LocalTime.of(22, 0), SP).toInstant();
        motorRegrasService.processarEvento(new EventoSensorMessageDto(
                UUID.randomUUID(), CASA_ID, null, TipoSensor.PRESENCA, Comodo.QUARTO, EstadoSensor.ATIVO, tOntem, true));

        // Manhã às 08:00 - ainda dentro da tolerância (07:20 + 90 min = 08:50) -> sem alerta
        Instant t0800 = ZonedDateTime.of(hoje, LocalTime.of(8, 0), SP).toInstant();
        motorRegrasService.processarAvancoRelogio(CASA_ID, t0800);
        assertTrue(maquinaEstadosService.buscarAlertaAberto(CASA_ID).isEmpty());

        // Manhã às 08:50 - tolerância esgotada -> dispara R1 ATENCAO
        Instant t0850 = ZonedDateTime.of(hoje, LocalTime.of(8, 50), SP).toInstant();
        motorRegrasService.processarAvancoRelogio(CASA_ID, t0850);

        Optional<Alerta> alertaOpt = maquinaEstadosService.buscarAlertaAberto(CASA_ID);
        assertTrue(alertaOpt.isPresent(), "Após 08:50 deve abrir alerta R1 por ausência de atividade matinal");

        Alerta alerta = alertaOpt.get();
        assertEquals(RegraTipo.R1, alerta.getRegra());
        assertEquals(GravidadeAlerta.ATENCAO, alerta.getGravidade());
    }
}
