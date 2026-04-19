package org.example;

import java.time.LocalDate;

public class TemporaryAssignment extends AbstractRoleAssignment{

    String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        ValidationUtils.requireNonEmpty(expiresAt, "Expiration date");
        if (!ValidationUtils.isValidDate(expiresAt)) {
            throw new IllegalArgumentException("Expiration date must be in format YYYY-MM-DD");
        }

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean isActive() {
        String today = LocalDate.now().toString();

        return expiresAt.compareTo(today) > 0;
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        if (!ValidationUtils.isValidDate(newExpirationDate)) {
            throw new IllegalArgumentException("New expiration date must be in format YYYY-MM-DD");
        }
        this.expiresAt = newExpirationDate;
    }

    public boolean isExpired(){
        return !isActive();
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "Expired";
        }

        return "Valid until: " + expiresAt;
    }

    @Override
    public String summary(){
        String baseSummary = super.summary();

        String expirationInfo = "Expires: " + expiresAt;

        if (autoRenew) {
            expirationInfo += " (auto-renew)";
        }

        return baseSummary + "\n" + expirationInfo;
    }

}
