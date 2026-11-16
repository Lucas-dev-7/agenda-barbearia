package br.com.agendasemfuro.appointment.dto;

import br.com.agendasemfuro.appointment.entity.Appointment;
import br.com.agendasemfuro.appointment.entity.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID barberId,
        UUID serviceId,
        String customerName,
        String customerPhone,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        AppointmentStatus status,
        LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(), appointment.getBarberId(), appointment.getServiceId(),
                appointment.getCustomerName(), appointment.getCustomerPhone(), appointment.getStartsAt(),
                appointment.getEndsAt(), appointment.getStatus(), appointment.getCreatedAt()
        );
    }
}
