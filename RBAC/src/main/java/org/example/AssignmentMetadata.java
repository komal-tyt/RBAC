package org.example;

import java.time.LocalDateTime;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public AssignmentMetadata {
        ValidationUtils.requireNonEmpty(assignedBy, "Assigned by");
        ValidationUtils.requireNonEmpty(assignedAt, "Assigned at");
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        ValidationUtils.requireNonEmpty(assignedBy, "Assigned by");
        String assignedAt = LocalDateTime.now().toString();
        return new AssignmentMetadata(assignedBy, assignedAt, reason);
    }

    public String format() {
        String result = "Assigned by: " + assignedBy + " at " + assignedAt;
        if (reason != null && !reason.isBlank()) {
            result += " (" + reason + ")";
        }
        return result;
    }
}