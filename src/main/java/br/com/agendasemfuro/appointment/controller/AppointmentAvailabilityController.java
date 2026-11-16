package br.com.agendasemfuro.appointment.controller;

import br.com.agendasemfuro.appointment.dto.AvailabilityResponse;
import br.com.agendasemfuro.appointment.service.AppointmentAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/barbers/{barberId}/availability")
public class AppointmentAvailabilityController {
    private final AppointmentAvailabilityService service;

    public AppointmentAvailabilityController(AppointmentAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    public AvailabilityResponse getAvailability(
            @PathVariable UUID barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId
    ) {
        return service.getAvailability(barberId, serviceId, date);
    }
}
