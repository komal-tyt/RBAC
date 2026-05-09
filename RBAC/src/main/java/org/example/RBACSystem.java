package org.example;

import java.util.Arrays;
import java.util.List;

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
