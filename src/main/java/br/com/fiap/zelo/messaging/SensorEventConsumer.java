package br.com.fiap.zelo.messaging;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.dto.RelogioMessageDto;
import br.com.fiap.zelo.service.MotorRegrasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensorEventConsumer {

    private final MotorRegrasService motorRegrasService;

    @RabbitListener(queues = "${app.zelo.rabbitmq.queue-eventos:zelo.sensor.eventos}")
    public void receberEvento(EventoSensorMessageDto evento) {
        log.info("Consumer recebeu evento de sensor: {}", evento);
        try {
            motorRegrasService.processarEvento(evento);
        } catch (Exception e) {
            log.error("Erro ao processar evento de sensor recebido da fila: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${app.zelo.rabbitmq.queue-relogio:zelo.sensor.relogio}")
    public void receberRelogio(RelogioMessageDto relogio) {
        log.info("Consumer recebeu avanço de relógio: casaId={}, agora={}", relogio.casaId(), relogio.agora());
        try {
            motorRegrasService.processarAvancoRelogio(relogio.casaId(), relogio.agora());
        } catch (Exception e) {
            log.error("Erro ao processar avanço de relógio recebido da fila: {}", e.getMessage(), e);
        }
    }
}
