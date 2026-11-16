package br.com.agendasemfuro.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "barber_working_hours")
public class WorkingHours {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "barber_id", length = 36, nullable = false, updatable = false)
    private UUID barberId;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    protected WorkingHours() {
    }

    public WorkingHours(UUID barberId, short dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.barberId = barberId;
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public UUID getId() { return id; }
    public UUID getBarberId() { return barberId; }
    public short getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
}
