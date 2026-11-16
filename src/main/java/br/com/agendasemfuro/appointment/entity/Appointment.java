package br.com.agendasemfuro.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "barber_id", length = 36, nullable = false, updatable = false)
    private UUID barberId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "service_id", length = 36, nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Appointment() {
    }

    public Appointment(UUID barberId, UUID serviceId, String customerName, String customerPhone,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        this.barberId = barberId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = AppointmentStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getBarberId() { return barberId; }
    public UUID getServiceId() { return serviceId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }
}
