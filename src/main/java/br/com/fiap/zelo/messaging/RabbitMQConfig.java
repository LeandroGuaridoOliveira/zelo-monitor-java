package br.com.fiap.zelo.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.zelo.rabbitmq.exchange:zelo}")
    private String exchangeName;

    @Value("${app.zelo.rabbitmq.queue-eventos:zelo.sensor.eventos}")
    private String queueEventosName;

    @Value("${app.zelo.rabbitmq.queue-relogio:zelo.sensor.relogio}")
    private String queueRelogioName;

    @Bean
    public TopicExchange zeloExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange amqTopicExchange() {
        return new TopicExchange("amq.topic", true, false);
    }

    @Bean
    public Queue queueEventos() {
        return new Queue(queueEventosName, true);
    }

    @Bean
    public Queue queueRelogio() {
        return new Queue(queueRelogioName, true);
    }

    @Bean
    public Queue queueNotificacoes() {
        return new Queue("zelo.alertas.notificacoes", true);
    }

    @Bean
    public Binding bindingEventosDirect(Queue queueEventos, TopicExchange zeloExchange) {
        return BindingBuilder.bind(queueEventos).to(zeloExchange).with("sensor.evento");
    }

    @Bean
    public Binding bindingEventosWildcard(Queue queueEventos, TopicExchange zeloExchange) {
        return BindingBuilder.bind(queueEventos).to(zeloExchange).with("zelo.casas.*.eventos");
    }

    @Bean
    public Binding bindingEventosMqtt(Queue queueEventos, TopicExchange amqTopicExchange) {
        return BindingBuilder.bind(queueEventos).to(amqTopicExchange).with("zelo.casas.*.eventos");
    }

    @Bean
    public Binding bindingRelogioDirect(Queue queueRelogio, TopicExchange zeloExchange) {
        return BindingBuilder.bind(queueRelogio).to(zeloExchange).with("sensor.relogio");
    }

    @Bean
    public Binding bindingRelogioWildcard(Queue queueRelogio, TopicExchange zeloExchange) {
        return BindingBuilder.bind(queueRelogio).to(zeloExchange).with("zelo.casas.*.relogio");
    }

    @Bean
    public Binding bindingRelogioMqtt(Queue queueRelogio, TopicExchange amqTopicExchange) {
        return BindingBuilder.bind(queueRelogio).to(amqTopicExchange).with("zelo.casas.*.relogio");
    }

    @Bean
    public Binding bindingNotificacoes(Queue queueNotificacoes, TopicExchange zeloExchange) {
        return BindingBuilder.bind(queueNotificacoes).to(zeloExchange).with("alerta.notificacao");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
        ObjectMapper configuredMapper = mapper.copy().registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(configuredMapper);
    }
}
