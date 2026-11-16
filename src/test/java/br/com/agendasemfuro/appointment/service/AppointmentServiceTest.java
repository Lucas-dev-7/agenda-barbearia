package br.com.agendasemfuro.appointment.service;

import br.com.agendasemfuro.appointment.dto.CreateAppointmentRequest;
import br.com.agendasemfuro.appointment.entity.Appointment;
import br.com.agendasemfuro.appointment.entity.AppointmentStatus;
import br.com.agendasemfuro.appointment.repository.AppointmentRepository;
import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import br.com.agendasemfuro.catalog.entity.ServiceOffering;
import br.com.agendasemfuro.catalog.repository.ServiceOfferingRepository;
import br.com.agendasemfuro.schedule.entity.WorkingHours;
import br.com.agendasemfuro.schedule.repository.WorkingHoursRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock private BarberRepository barberRepository;
    @Mock private ServiceOfferingRepository serviceOfferingRepository;
    @Mock private WorkingHoursRepository workingHoursRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private Barber barber;
    @Mock private ServiceOffering serviceOffering;
    @Mock private Appointment appointment;

    private AppointmentService service;
    private UUID barberId;
    private UUID serviceId;
    private UUID appointmentId;
    private LocalDateTime startsAt;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(
                barberRepository, serviceOfferingRepository, workingHoursRepository, appointmentRepository
        );
        barberId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        startsAt = LocalDate.now(BUSINESS_ZONE).plusDays(2).atTime(10, 0);
    }

    private void prepareCreate() {
        when(barberRepository.findByIdForUpdate(barberId)).thenReturn(Optional.of(barber));
        when(barber.getId()).thenReturn(barberId);
        when(barber.isActive()).thenReturn(true);
        when(serviceOfferingRepository.findById(serviceId)).thenReturn(Optional.of(serviceOffering));
        when(serviceOffering.isActive()).thenReturn(true);
        when(serviceOffering.getDurationMinutes()).thenReturn(30);
    }

    private void stubWorkingHours(LocalTime opensAt, LocalTime closesAt) {
        when(workingHoursRepository.findAllByBarberIdAndDayOfWeekOrderByOpensAtAsc(
                barberId, (short) startsAt.getDayOfWeek().getValue()
        )).thenReturn(List.of(new WorkingHours(
                barberId, (short) startsAt.getDayOfWeek().getValue(), opensAt, closesAt
        )));
    }

    @Test
    void createsAppointmentInsideWorkingHoursWhenSlotIsFree() {
        prepareCreate();
        when(serviceOffering.getId()).thenReturn(serviceId);
        stubWorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(appointmentRepository.existsByBarberIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                barberId, AppointmentStatus.SCHEDULED, startsAt.plusMinutes(30), startsAt
        )).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment created = service.create(new CreateAppointmentRequest(
                barberId, serviceId, "  Ana Souza  ", "  11999999999  ", startsAt
        ));

        assertThat(created.getBarberId()).isEqualTo(barberId);
        assertThat(created.getServiceId()).isEqualTo(serviceId);
        assertThat(created.getCustomerName()).isEqualTo("Ana Souza");
        assertThat(created.getCustomerPhone()).isEqualTo("11999999999");
        assertThat(created.getStartsAt()).isEqualTo(startsAt);
        assertThat(created.getEndsAt()).isEqualTo(startsAt.plusMinutes(30));
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void rejectsOverlappingAppointment() {
        prepareCreate();
        stubWorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(appointmentRepository.existsByBarberIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                barberId, AppointmentStatus.SCHEDULED, startsAt.plusMinutes(30), startsAt
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void rejectsAppointmentOutsideWorkingHours() {
        prepareCreate();
        startsAt = startsAt.withHour(17).withMinute(45);
        stubWorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void cancelsScheduledAppointment() {
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);

        assertThat(service.cancel(appointmentId)).isSameAs(appointment);

        verify(appointment).cancel();
    }

    @Test
    void rejectsCancellationWhenAppointmentIsNotScheduled() {
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancel(appointmentId))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));

        verify(appointment, never()).cancel();
    }

    private CreateAppointmentRequest validRequest() {
        return new CreateAppointmentRequest(barberId, serviceId, "Ana Souza", "11999999999", startsAt);
    }
}
