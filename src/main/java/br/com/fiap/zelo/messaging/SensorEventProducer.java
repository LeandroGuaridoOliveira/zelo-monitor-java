package br.com.fiap.zelo.messaging;

import br.com.fiap.zelo.dto.EventoSensorMessageDto;
import br.com.fiap.zelo.dto.RelogioMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventProducer {

    private final AmqpTemplate amqpTemplate;

    @Value("${app.zelo.rabbitmq.exchange:zelo}")
    private String exchange;

    @Value("${app.zelo.rabbitmq.routing-key-evento:sensor.evento}")
    private String routingKeyEvento;

    @Value("${app.zelo.rabbitmq.routing-key-relogio:sensor.relogio}")
    private String routingKeyRelogio;

    public void publicarEvento(EventoSensorMessageDto evento) {
        try {
            amqpTemplate.convertAndSend(exchange, routingKeyEvento, evento);
            log.info("Evento de sensor publicado no RabbitMQ: comodo={}, estado={}, casaId={}",
                    evento.comodo(), evento.estado(), evento.casaId());
        } catch (Exception e) {
            log.warn("Erro ao publicar evento no RabbitMQ: {}", e.getMessage());
        }
    }

    public void publicarRelogio(RelogioMessageDto relogio) {
        try {
            amqpTemplate.convertAndSend(exchange, routingKeyRelogio, relogio);
            log.info("Avanço de relógio publicado no RabbitMQ: agora={}, casaId={}",
                    relogio.agora(), relogio.casaId());
        } catch (Exception e) {
            log.warn("Erro ao publicar relógio no RabbitMQ: {}", e.getMessage());
        }
    }

    public void publicarNotificacaoAlerta(Object notificacao) {
        try {
            amqpTemplate.convertAndSend(exchange, "alerta.notificacao", notificacao);
            log.info("Notificação de alerta publicada no RabbitMQ");
        } catch (Exception e) {
            log.warn("Erro ao publicar notificação no RabbitMQ: {}", e.getMessage());
        }
    }
}
