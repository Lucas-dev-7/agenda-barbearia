package br.com.agendasemfuro.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationListener.class);

    @RabbitListener(queues = "${app.messaging.rabbitmq.queue}")
    public void consume(AppointmentCreatedEvent event) {
        log.info("Simulated appointment confirmation consumed for appointment {} starting at {}",
                event.appointmentId(), event.startsAt());
    }
}
