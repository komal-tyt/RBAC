package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentManagerTest {

    private AssignmentManager assignmentManager;

    @Mock
    private UserManager userManager;

    @Mock
    private RoleManager roleManager;

    private User testUser;
    private Role adminRole;
    private Role userRole;
    private Permission readPermission;
    private AssignmentMetadata metadata;
    private PermanentAssignment permanentAssignment;
    private TemporaryAssignment temporaryAssignment;

    @BeforeEach
    void setUp() {
        assignmentManager = new AssignmentManager(userManager, roleManager);

        testUser = User.create("testuser", "Test User", "test@example.com");
        adminRole = new Role("ADMIN", "Administrator role");
        userRole = new Role("USER", "Regular user role");

        readPermission = new Permission("READ", "document", "Can read documents");
        adminRole.addPermission(readPermission);
        userRole.addPermission(readPermission);

        metadata = AssignmentMetadata.now("admin", "Test assignment");

        permanentAssignment = new PermanentAssignment(testUser, adminRole, metadata);
        temporaryAssignment = new TemporaryAssignment(testUser, userRole, metadata, "2025-12-31", false);
    }

    @Test
    void add_ShouldAddAssignment_WhenValid() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(permanentAssignment.assignmentId()).isPresent());
    }

    @Test
    void add_ShouldThrowException_WhenUserDoesNotExist() {
        when(userManager.exists("testuser")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(permanentAssignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void add_ShouldThrowException_WhenRoleDoesNotExist() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(permanentAssignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void add_ShouldThrowException_WhenDuplicateActiveAssignment() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        PermanentAssignment duplicate = new PermanentAssignment(testUser, adminRole, metadata);

        assertThrows(IllegalStateException.class, () -> assignmentManager.add(duplicate));
        assertEquals(1, assignmentManager.count());
    }

    @Test
    void remove_ShouldReturnTrue_WhenAssignmentExists() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        assertTrue(assignmentManager.remove(permanentAssignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void remove_ShouldReturnFalse_WhenAssignmentIsNull() {
        assertFalse(assignmentManager.remove(null));
    }

    @Test
    void findById_ShouldReturnAssignment_WhenExists() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        Optional<RoleAssignment> found = assignmentManager.findById(permanentAssignment.assignmentId());

        assertTrue(found.isPresent());
        assertEquals(permanentAssignment.assignmentId(), found.get().assignmentId());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        assertFalse(assignmentManager.findById("nonexistent-id").isPresent());
    }

    @Test
    void findAll_ShouldReturnAllAssignments() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assignmentManager.add(temporaryAssignment);

        List<RoleAssignment> allAssignments = assignmentManager.findAll();

        assertEquals(2, allAssignments.size());
        assertTrue(allAssignments.contains(permanentAssignment));
        assertTrue(allAssignments.contains(temporaryAssignment));
    }

    @Test
    void count_ShouldReturnCorrectNumber() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assertEquals(0, assignmentManager.count());

        assignmentManager.add(permanentAssignment);
        assertEquals(1, assignmentManager.count());
    }

    @Test
    void clear_ShouldRemoveAllAssignments() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assignmentManager.add(temporaryAssignment);

        assignmentManager.clear();

        assertEquals(0, assignmentManager.count());
    }

    @Test
    void findByUser_ShouldReturnUserAssignments() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assignmentManager.add(temporaryAssignment);

        List<RoleAssignment> userAssignments = assignmentManager.findByUser(testUser);

        assertEquals(2, userAssignments.size());
    }

    @Test
    void findByRole_ShouldReturnRoleAssignments() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assignmentManager.add(temporaryAssignment);

        List<RoleAssignment> adminAssignments = assignmentManager.findByRole(adminRole);

        assertEquals(1, adminAssignments.size());
    }

    @Test
    void findByFilter_ShouldReturnFilteredAssignments() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assignmentManager.add(temporaryAssignment);

        AssignmentFilter permanentFilter = a -> a.assignmentType().equals("PERMANENT");
        List<RoleAssignment> permanentAssignments = assignmentManager.findByFilter(permanentFilter);

        assertEquals(1, permanentAssignments.size());
        assertTrue(permanentAssignments.get(0) instanceof PermanentAssignment);
    }

    @Test
    void getActiveAssignments_ShouldReturnOnlyActive() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        TemporaryAssignment expiredTemp = new TemporaryAssignment(testUser, userRole, metadata, "2020-01-01", false);
        assignmentManager.add(expiredTemp);

        List<RoleAssignment> active = assignmentManager.getActiveAssignments();

        assertEquals(1, active.size());
        assertTrue(active.get(0) instanceof PermanentAssignment);
    }

    @Test
    void getExpiredAssignments_ShouldReturnOnlyExpired() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        TemporaryAssignment expiredTemp = new TemporaryAssignment(testUser, userRole, metadata, "2020-01-01", false);
        assignmentManager.add(expiredTemp);

        List<RoleAssignment> expired = assignmentManager.getExpiredAssignments();

        assertEquals(1, expired.size());
        assertTrue(expired.get(0) instanceof TemporaryAssignment);
    }

    @Test
    void userHasRole_ShouldReturnTrue_WhenUserHasActiveRole() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        assertTrue(assignmentManager.userHasRole(testUser, adminRole));
        assertFalse(assignmentManager.userHasRole(testUser, userRole));
    }

    @Test
    void userHasPermission_ShouldReturnTrue_WhenUserHasPermission() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        assertTrue(assignmentManager.userHasPermission(testUser, "READ", "document"));
        assertFalse(assignmentManager.userHasPermission(testUser, "WRITE", "document"));
    }

    @Test
    void getUserPermissions_ShouldReturnAllPermissionsFromAllRoles() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        Set<Permission> permissions = assignmentManager.getUserPermissions(testUser);

        assertEquals(1, permissions.size());
        assertTrue(permissions.contains(readPermission));
    }

    @Test
    void revokeAssignment_ShouldRevokePermanentAssignment() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);
        assertTrue(permanentAssignment.isActive());

        assignmentManager.revokeAssignment(permanentAssignment.assignmentId());

        assertFalse(permanentAssignment.isActive());
    }

    @Test
    void revokeAssignment_ShouldExpireTemporaryAssignment() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(temporaryAssignment);
        assertTrue(temporaryAssignment.isActive());

        assignmentManager.revokeAssignment(temporaryAssignment.assignmentId());

        assertFalse(temporaryAssignment.isActive());
    }

    @Test
    void revokeAssignment_ShouldThrowException_WhenAssignmentNotFound() {
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.revokeAssignment("nonexistent-id"));
    }

    @Test
    void extendTemporaryAssignment_ShouldExtendExpirationDate() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("USER")).thenReturn(true);

        assignmentManager.add(temporaryAssignment);
        String newExpiration = "2026-12-31";

        assignmentManager.extendTemporaryAssignment(temporaryAssignment.assignmentId(), newExpiration);

        assertTrue(temporaryAssignment.getTimeRemaining().contains(newExpiration));
    }

    @Test
    void extendTemporaryAssignment_ShouldThrowException_WhenAssignmentNotTemporary() {
        when(userManager.exists("testuser")).thenReturn(true);
        when(roleManager.exists("ADMIN")).thenReturn(true);

        assignmentManager.add(permanentAssignment);

        assertThrows(IllegalArgumentException.class, () ->
                assignmentManager.extendTemporaryAssignment(permanentAssignment.assignmentId(), "2026-12-31"));
    }
}