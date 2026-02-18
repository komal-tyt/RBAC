package org.example;

import java.time.LocalDate;

public class TemporaryAssignment extends AbstractRoleAssignment{

    private String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
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

    public void extend(String newExpirationDate){
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
