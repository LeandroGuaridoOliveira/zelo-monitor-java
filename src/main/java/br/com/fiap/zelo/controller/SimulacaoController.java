package br.com.fiap.zelo.controller;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.dto.RelogioMessageDto;
import br.com.fiap.zelo.enums.Comodo;
import br.com.fiap.zelo.enums.EstadoCasa;
import br.com.fiap.zelo.enums.EstadoSensor;
import br.com.fiap.zelo.enums.TipoSensor;
import br.com.fiap.zelo.messaging.SensorEventProducer;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.repository.CasaMonitoradaRepository;
import br.com.fiap.zelo.service.MotorRegrasService;
import br.com.fiap.zelo.service.RelogioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulacao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Simulação e Testes de Campo", description = "Endpoints de disparo manual e simulação dos Roteiros 1 a 4")
public class SimulacaoController {

    private final MotorRegrasService motorRegrasService;
    private final RelogioService relogioService;
    private final SensorEventProducer sensorEventProducer;
    private final CasaMonitoradaRepository casaRepository;

    private static final UUID CASA_DONA_MARIA = UUID.fromString("00000000-0000-0000-0000-000000000100");

    @PostMapping("/evento")
    @Operation(summary = "Dispara um evento de sensor na simulação (processa localmente e publica no RabbitMQ)")
    public ResponseEntity<Map<String, String>> dispararEvento(@RequestBody EventoSensorMessageDto evento) {
        sensorEventProducer.publicarEvento(evento);
        motorRegrasService.processarEvento(evento);
        return ResponseEntity.ok(Map.of("status", "EVENTO_PROCESSADO", "comodo", evento.comodo().name()));
    }

    @PostMapping("/relogio")
    @Operation(summary = "Avança o relógio simulado da casa e avalia timeouts de regras e alertas")
    public ResponseEntity<Map<String, String>> avancarRelogio(@RequestBody RelogioMessageDto relogio) {
        sensorEventProducer.publicarRelogio(relogio);
        motorRegrasService.processarAvancoRelogio(relogio.casaId(), relogio.agora());
        return ResponseEntity.ok(Map.of("status", "RELOGIO_AVANCADO", "agora", relogio.agora().toString()));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reseta o estado simulado da casa para o padrão inicial")
    public ResponseEntity<Map<String, String>> resetar(@RequestParam(required = false) UUID casaId) {
        UUID id = casaId != null ? casaId : CASA_DONA_MARIA;
        relogioService.resetar(id);

        casaRepository.findById(id).ifPresent(casa -> {
            casa.setEstadoCasa(EstadoCasa.EM_CASA);
            casa.setPortaEntradaAbertaEm(null);
            casa.setUltimoComodo(null);
            casa.setUltimoEstadoSensor(null);
            casa.setUltimoEventoEm(null);
            casaRepository.save(casa);
        });

        return ResponseEntity.ok(Map.of("status", "RESET_CONCLUIDO", "casaId", id.toString()));
    }

    @PostMapping("/roteiro/{numero}")
    @Operation(summary = "Executa um dos 4 roteiros oficiais de teste (1: Dia normal, 2: Ida à feira, 3: Queda banheiro R2, 4: Sem atividade R1)")
    public ResponseEntity<Map<String, Object>> executarRoteiro(
            @PathVariable int numero,
            @RequestParam(required = false) UUID casaId) {

        UUID id = casaId != null ? casaId : CASA_DONA_MARIA;
        ZoneId sp = ZoneId.of("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now();

        switch (numero) {
            case 1 -> {
                // Roteiro 1: Dia normal
                Instant t1 = ZonedDateTime.of(hoje, LocalTime.of(7, 30), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PRESENCA, Comodo.QUARTO, EstadoSensor.ATIVO, t1, true));

                Instant t2 = ZonedDateTime.of(hoje, LocalTime.of(7, 45), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PRESENCA, Comodo.BANHEIRO, EstadoSensor.ATIVO, t2, true));

                Instant t3 = ZonedDateTime.of(hoje, LocalTime.of(8, 0), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PRESENCA, Comodo.COZINHA, EstadoSensor.ATIVO, t3, true));

                return ResponseEntity.ok(Map.of(
                        "roteiro", 1,
                        "descricao", "Dia normal: Dona Maria acordou às 07:30, foi ao banheiro e depois para a cozinha. Nenhum alerta aberto.",
                        "status", "SUCESSO"
                ));
            }
            case 2 -> {
                // Roteiro 2: Ida à feira
                Instant saida = ZonedDateTime.of(hoje, LocalTime.of(8, 30), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PORTA, Comodo.ENTRADA, EstadoSensor.ABERTA, saida, true));

                // 11 minutos depois avança o relógio -> FORA_DE_CASA
                Instant tFora = ZonedDateTime.of(hoje, LocalTime.of(8, 41), sp).toInstant();
                motorRegrasService.processarAvancoRelogio(id, tFora);

                // 2 horas depois: silêncio sem alertas
                Instant tApos2h = ZonedDateTime.of(hoje, LocalTime.of(10, 30), sp).toInstant();
                motorRegrasService.processarAvancoRelogio(id, tApos2h);

                return ResponseEntity.ok(Map.of(
                        "roteiro", 2,
                        "descricao", "Ida à feira: Porta de entrada aberta às 08:30. Às 08:41 morador foi classificado como FORA_DE_CASA. Monitoramento suspenso com sucesso por 2 horas.",
                        "status", "SUCESSO"
                ));
            }
            case 3 -> {
                // Roteiro 3: Queda no banheiro (R2)
                Instant entradaBanheiro = ZonedDateTime.of(hoje, LocalTime.of(8, 15), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PRESENCA, Comodo.BANHEIRO, EstadoSensor.ATIVO, entradaBanheiro, true));

                // Aos 39 min: relógio avança sem abrir alerta
                Instant t39 = entradaBanheiro.plusSeconds(39 * 60);
                motorRegrasService.processarAvancoRelogio(id, t39);

                // Aos 40 min: relógio avança e abre alerta R2 CRITICO em AGUARDANDO_IDOSO
                Instant t40 = entradaBanheiro.plusSeconds(40 * 60);
                motorRegrasService.processarAvancoRelogio(id, t40);

                return ResponseEntity.ok(Map.of(
                        "roteiro", 3,
                        "descricao", "Queda no banheiro (R2): Dona Maria entrou às 08:15. Aos 39 min sem alerta. Aos 40 min alerta R2 CRÍTICO aberto aguardando idoso.",
                        "status", "SUCESSO"
                ));
            }
            case 4 -> {
                // Roteiro 4: Sem atividade matinal (R1)
                Instant ontemNoite = ZonedDateTime.of(hoje.minusDays(1), LocalTime.of(22, 0), sp).toInstant();
                motorRegrasService.processarEvento(new EventoSensorMessageDto(UUID.randomUUID(), id, null, TipoSensor.PRESENCA, Comodo.QUARTO, EstadoSensor.ATIVO, ontemNoite, true));

                // Silêncio matinal até 08:50 (07:20 habitual + 90 min tolerância)
                Instant tLimite = ZonedDateTime.of(hoje, LocalTime.of(8, 50), sp).toInstant();
                motorRegrasService.processarAvancoRelogio(id, tLimite);

                return ResponseEntity.ok(Map.of(
                        "roteiro", 4,
                        "descricao", "Sem atividade matinal (R1): Silêncio detectado após as 08:50 (07:20 habitual + 90 min). Alerta R1 ATENÇÃO aberto com sucesso.",
                        "status", "SUCESSO"
                ));
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("erro", "Número de roteiro inválido (utilize 1, 2, 3 ou 4)"));
            }
        }
    }
}
