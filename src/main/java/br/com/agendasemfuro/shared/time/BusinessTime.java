package br.com.agendasemfuro.shared.time;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class BusinessTime {
    public static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private BusinessTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }
}
