package br.com.agendasemfuro.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
        UUID appointmentId,
        String customerName,
        String customerPhone,
        String barberName,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
