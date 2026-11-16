package br.com.agendasemfuro.appointment.controller;

import br.com.agendasemfuro.appointment.dto.AppointmentResponse;
import br.com.agendasemfuro.appointment.dto.CreateAppointmentRequest;
import br.com.agendasemfuro.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return AppointmentResponse.from(service.create(request));
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse get(@PathVariable UUID appointmentId) {
        return AppointmentResponse.from(service.get(appointmentId));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID appointmentId) {
        return AppointmentResponse.from(service.cancel(appointmentId));
    }
}
