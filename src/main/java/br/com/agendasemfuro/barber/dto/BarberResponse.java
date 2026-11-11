package br.com.agendasemfuro.barber.dto;

import br.com.agendasemfuro.barber.entity.Barber;

import java.time.LocalDateTime;
import java.util.UUID;

public record BarberResponse(UUID id, String name, boolean active, LocalDateTime createdAt) {
    public static BarberResponse from(Barber barber) {
        return new BarberResponse(barber.getId(), barber.getName(), barber.isActive(), barber.getCreatedAt());
    }
}
