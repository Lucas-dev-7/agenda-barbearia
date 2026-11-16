package br.com.agendasemfuro.appointment.repository;

import br.com.agendasemfuro.appointment.entity.Appointment;
import br.com.agendasemfuro.appointment.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    boolean existsByBarberIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID barberId,
            AppointmentStatus status,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);
}
