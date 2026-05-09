package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final BackgroundExecutor backgroundExecutor;
    private String currentUser;

    public RBACSystem(AuditLog auditLog){
        this.userManager = new UserManager();
        this.assignmentManager = new AssignmentManager(userManager, null);
        this.roleManager = new RoleManager(assignmentManager);
        this.assignmentManager.setRoleManager(roleManager);
        this.auditLog = auditLog;
        this.backgroundExecutor = new BackgroundExecutor();
        this.currentUser = null;

        startScheduledTasks();
    }

    private void startScheduledTasks() {
        backgroundExecutor.scheduleAtFixedRate(() -> {
            try {
                expireTemporaryAssignments();
            } catch (Exception e) {
                System.err.println("Error in expire task: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

        backgroundExecutor.scheduleAtFixedRate(() -> {
            try {
                logStatistics();
            } catch (Exception e) {
                System.err.println("Error in stats task: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);
    }

    private void expireTemporaryAssignments() {
        String currentDate = DateUtils.getCurrentDate();
        int expiredCount = 0;


        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        for (RoleAssignment ra : allAssignments) {
            if (ra instanceof TemporaryAssignment) {
                TemporaryAssignment temp = (TemporaryAssignment) ra;
                if (temp.isActive() && DateUtils.isBefore(temp.getExpiresAt(), currentDate)) {
                    String yesterday = DateUtils.addDays(currentDate, -1);
                    temp.extend(yesterday);
                    expiredCount++;

                    auditLog.log("EXPIRED_ASSIGNMENT", "system",
                            temp.user().username() + " -> " + temp.role().name(),
                            "Assignment expired on " + temp.getExpiresAt());
                }
            }
        }

        if (expiredCount > 0) {
            System.out.println("[SCHEDULED] Expired " + expiredCount + " temporary assignments");
        }
    }

    private void logStatistics() {
        int userCount = userManager.count();
        int roleCount = roleManager.count();
        int assignmentCount = assignmentManager.count();
        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();

        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        long permanentCount = allAssignments.stream()
                .filter(ra -> ra instanceof PermanentAssignment)
                .count();
        long temporaryCount = allAssignments.stream()
                .filter(ra -> ra instanceof TemporaryAssignment)
                .count();

        String stats = String.format(
                "[SCHEDULED STATS] Users: %d, Roles: %d, Assignments: %d (Permanent: %d, Temporary: %d, Active: %d, Expired: %d)",
                userCount, roleCount, assignmentCount, permanentCount, temporaryCount, activeAssignments, expiredAssignments
        );

        System.out.println(stats);
        auditLog.log("SCHEDULED_STATS", "system", "system", stats);
    }

    public BackgroundExecutor getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public void shutdown() {
        backgroundExecutor.shutdown();
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public void initialize(){
        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");

        Permission readReports = new Permission("READ", "reports", "Read reports");
        Permission writeReports = new Permission("WRITE", "reports", "Write reports");
        Permission deleteReports = new Permission("DELETE", "reports", "Delete reports");

        Permission readSettings = new Permission("READ", "settings", "Read settings");
        Permission writeSettings = new Permission("WRITE", "settings", "Write settings");
        Permission deleteSettings = new Permission("DELETE", "settings", "Delete settings");

        List<Permission> allPermissions = Arrays.asList(
                readUsers, writeUsers, deleteUsers,
                readReports, writeReports, deleteReports,
                readSettings, writeSettings, deleteSettings
        );

        Role adminRole = new Role("Admin", "Full system access");
        allPermissions.forEach(adminRole::addPermission);

        List<Permission> managerPermissions = Arrays.asList(
                readUsers, writeUsers,
                readReports, writeReports,
                readSettings
        );

        Role managerRole = new Role("Manager", "Manage users, reports");
        managerPermissions.forEach(managerRole::addPermission);

        List<Permission> viewerPermissions = Arrays.asList(
                readUsers, readReports, readSettings
        );

        Role viewerRole = new Role("Viewer", "Read-only access");
        viewerPermissions.forEach(viewerRole::addPermission);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);


        User admin = User.create("admin", "System Administrator", "admin@example.com");
        userManager.add(admin);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Initial admin assignment");
        PermanentAssignment adminAssignment = new PermanentAssignment(admin, adminRole, metadata);
        assignmentManager.add(adminAssignment);

        setCurrentUser("admin");
    }

    public String generateStatistics() {
        return "=== SYSTEM STATISTICS ===\n" +
                "Current user: " + (currentUser != null ? currentUser : "none") + "\n" +
                "Total users: " + userManager.count() + "\n" +
                "Total roles: " + roleManager.count() + "\n" +
                "Total assignments: " + assignmentManager.count() + "\n" +
                "=========================";
    }
}
