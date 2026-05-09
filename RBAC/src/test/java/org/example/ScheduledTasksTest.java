package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScheduledTasksTest {

    @Test
    void testExpiredAssignmentsDetection() throws InterruptedException {
        System.out.println("\n========== EXPIRED ASSIGNMENTS TEST ==========");

        AuditLog auditLog = new AuditLog();
        RBACSystem system = new RBACSystem(auditLog);
        system.initialize();

        User user = User.create("test_expired", "Test User", "expired@test.com");
        system.getUserManager().add(user);

        Role role = new Role("ExpiredTestRole", "Test role");
        system.getRoleManager().add(role);

        String expiredDate = DateUtils.addDays(DateUtils.getCurrentDate(), -5);
        AssignmentMetadata meta = AssignmentMetadata.now("system", "Expired test");
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, expiredDate, false);
        system.getAssignmentManager().add(assignment);

        assertFalse(assignment.isActive(), "Assignment should be expired immediately");

        System.out.println("Expired assignment created: " + assignment.getExpiresAt());
        System.out.println("Is active: " + assignment.isActive());

        System.out.println("========== EXPIRED ASSIGNMENTS TEST PASSED ==========");

        system.shutdown();
        Thread.sleep(500);
    }

    @Test
    void testStatisticsLogging() throws InterruptedException {
        System.out.println("\n========== STATISTICS LOGGING TEST ==========");

        AuditLog auditLog = new AuditLog();
        RBACSystem system = new RBACSystem(auditLog);
        system.initialize();

        User user1 = User.create("stat_user1", "Stat User 1", "stat1@test.com");
        User user2 = User.create("stat_user2", "Stat User 2", "stat2@test.com");
        system.getUserManager().add(user1);
        system.getUserManager().add(user2);

        Role role = new Role("StatRole", "Statistics role");
        system.getRoleManager().add(role);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Stat assignment");
        TemporaryAssignment assignment = new TemporaryAssignment(user1, role, meta, DateUtils.addDays(DateUtils.getCurrentDate(), 30), false);
        system.getAssignmentManager().add(assignment);

        Thread.sleep(2000);

        int userCount = system.getUserManager().count();
        int roleCount = system.getRoleManager().count();
        int assignmentCount = system.getAssignmentManager().count();

        System.out.println("Current statistics:");
        System.out.println("  Users: " + userCount);
        System.out.println("  Roles: " + roleCount);
        System.out.println("  Assignments: " + assignmentCount);

        assertTrue(userCount >= 3, "Should have at least 3 users (admin + 2 test users)");
        assertTrue(roleCount >= 4, "Should have at least 4 roles (Admin, Manager, Viewer, StatRole)");

        System.out.println("========== STATISTICS LOGGING TEST PASSED ==========");

        system.shutdown();
        Thread.sleep(500);
    }
}