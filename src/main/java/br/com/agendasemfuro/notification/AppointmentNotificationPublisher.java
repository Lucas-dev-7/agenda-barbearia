package br.com.agendasemfuro.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "app.messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentNotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public AppointmentNotificationPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.rabbitmq.exchange}") String exchange,
            @Value("${app.messaging.rabbitmq.routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AppointmentCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published confirmation event for appointment {}", event.appointmentId());
        } catch (AmqpException exception) {
            log.warn("Could not publish confirmation event for appointment {}. The appointment is saved; no outbox retry is configured.",
                    event.appointmentId(), exception);
        }
    }
}
