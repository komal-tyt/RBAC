package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AuditEntry(
        String timestamp,
        String action,
        String performer,
        String target,
        String details
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static AuditEntry create(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return new AuditEntry(timestamp, action, performer, target, details);
    }

    public String format() {
        return String.format("[%s] %s | by: %s | target: %s | %s",
                timestamp, action, performer, target, details);
    }
}