package org.example;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        ValidationUtils.requireNonEmpty(expiresAt, "Expiration date");
        if (!DateUtils.isValidDateFormat(expiresAt)) {
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
        return DateUtils.isAfter(expiresAt, DateUtils.getCurrentDate());
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        ValidationUtils.requireNonEmpty(newExpirationDate, "New expiration date");
        if (!DateUtils.isValidDateFormat(newExpirationDate)) {
            throw new IllegalArgumentException("New expiration date must be in format YYYY-MM-DD");
        }
        this.expiresAt = newExpirationDate;
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            String expiredDate = expiresAt;
            if (DateUtils.isBefore(expiresAt, DateUtils.getCurrentDate())) {
                expiredDate = expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
            }
            return "Expired on " + expiredDate;
        }
        return "Valid until: " + expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        String expirationInfo = "Expires: " + expiresAt;
        if (autoRenew) {
            expirationInfo += " (auto-renew)";
        }
        expirationInfo += " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
        return baseSummary + "\n" + expirationInfo;
    }
}