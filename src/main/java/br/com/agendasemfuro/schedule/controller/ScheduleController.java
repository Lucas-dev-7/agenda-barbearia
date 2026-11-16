package br.com.agendasemfuro.schedule.controller;

import br.com.agendasemfuro.schedule.dto.CreateWorkingHoursRequest;
import br.com.agendasemfuro.schedule.dto.WorkingHoursResponse;
import br.com.agendasemfuro.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/barbers/{barberId}")
public class ScheduleController {
    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping("/working-hours")
    public List<WorkingHoursResponse> listWorkingHours(@PathVariable UUID barberId) {
        return service.listWorkingHours(barberId).stream().map(WorkingHoursResponse::from).toList();
    }

    @PostMapping("/working-hours")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkingHoursResponse addWorkingHours(
            @PathVariable UUID barberId,
            @Valid @RequestBody CreateWorkingHoursRequest request
    ) {
        return WorkingHoursResponse.from(service.addWorkingHours(barberId, request));
    }
}
