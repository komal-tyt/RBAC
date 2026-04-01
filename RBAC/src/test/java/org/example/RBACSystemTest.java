package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {

    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
    }

    @Test
    void testConstructor() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());
        assertNull(system.getCurrentUser());
    }

    @Test
    void testInitializeCreatesAdminUser() {
        system.initialize();
        assertTrue(system.getUserManager().exists("admin"));
    }

    @Test
    void testInitializeCreatesDefaultRoles() {
        system.initialize();
        assertTrue(system.getRoleManager().exists("Admin"));
        assertTrue(system.getRoleManager().exists("Manager"));
        assertTrue(system.getRoleManager().exists("Viewer"));
    }

    @Test
    void testInitializeAssignsAdminRole() {
        system.initialize();
        User admin = system.getUserManager().findByUsername("admin").get();
        Role adminRole = system.getRoleManager().findByName("Admin").get();
        assertTrue(system.getAssignmentManager().userHasRole(admin, adminRole));
    }

    @Test
    void testInitializeSetsCurrentUser() {
        system.initialize();
        assertEquals("admin", system.getCurrentUser());
    }

    @Test
    void testGenerateStatisticsAfterInitialize() {
        system.initialize();
        String stats = system.generateStatistics();
        assertTrue(stats.contains("Current user: admin"));
        assertTrue(stats.contains("Total users: 1"));
        assertTrue(stats.contains("Total roles: 3"));
        assertTrue(stats.contains("Total assignments: 1"));
    }

    @Test
    void testGenerateStatisticsEmptySystem() {
        String stats = system.generateStatistics();
        assertTrue(stats.contains("Current user: none"));
        assertTrue(stats.contains("Total users: 0"));
        assertTrue(stats.contains("Total roles: 0"));
        assertTrue(stats.contains("Total assignments: 0"));
    }

    @Test
    void testSetCurrentUser() {
        system.setCurrentUser("testuser");
        assertEquals("testuser", system.getCurrentUser());
    }

    @Test
    void testGetCurrentUserInitiallyNull() {
        assertNull(system.getCurrentUser());
    }

    @Test
    void testGetUserManager() {
        assertNotNull(system.getUserManager());
    }

    @Test
    void testGetRoleManager() {
        assertNotNull(system.getRoleManager());
    }

    @Test
    void testGetAssignmentManager() {
        assertNotNull(system.getAssignmentManager());
    }

    @Test
    void testAdminHasAllPermissions() {
        system.initialize();
        User admin = system.getUserManager().findByUsername("admin").get();

        assertTrue(system.getAssignmentManager().userHasPermission(admin, "READ", "users"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "WRITE", "users"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "DELETE", "users"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "READ", "reports"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "WRITE", "reports"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "DELETE", "reports"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "READ", "settings"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "WRITE", "settings"));
        assertTrue(system.getAssignmentManager().userHasPermission(admin, "DELETE", "settings"));
    }
}