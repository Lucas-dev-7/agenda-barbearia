CREATE TABLE barbers (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE services (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_services_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_services_price CHECK (price >= 0)
);

CREATE TABLE barber_working_hours (
    id CHAR(36) PRIMARY KEY,
    barber_id CHAR(36) NOT NULL,
    day_of_week SMALLINT NOT NULL,
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    CONSTRAINT fk_working_hours_barber FOREIGN KEY (barber_id) REFERENCES barbers (id),
    CONSTRAINT chk_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_working_hours_range CHECK (closes_at > opens_at)
);

CREATE INDEX ix_barber_working_hours_barber_day
    ON barber_working_hours (barber_id, day_of_week);

CREATE TABLE appointments (
    id CHAR(36) PRIMARY KEY,
    barber_id CHAR(36) NOT NULL,
    service_id CHAR(36) NOT NULL,
    customer_name VARCHAR(120) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_appointments_barber FOREIGN KEY (barber_id) REFERENCES barbers (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT chk_appointments_range CHECK (ends_at > starts_at)
);

CREATE INDEX ix_appointments_barber_start
    ON appointments (barber_id, starts_at);

CREATE INDEX ix_appointments_status
    ON appointments (status);