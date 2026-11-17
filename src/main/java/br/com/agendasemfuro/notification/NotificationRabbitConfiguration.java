package br.com.agendasemfuro.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRabbitConfiguration {
    @Bean
    TopicExchange appointmentEventsExchange(
            @Value("${app.messaging.rabbitmq.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue appointmentCreatedQueue(@Value("${app.messaging.rabbitmq.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding appointmentCreatedBinding(
            Queue appointmentCreatedQueue,
            TopicExchange appointmentEventsExchange,
            @Value("${app.messaging.rabbitmq.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(appointmentCreatedQueue)
                .to(appointmentEventsExchange)
                .with(routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
