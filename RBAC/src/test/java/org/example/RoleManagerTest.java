package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleManagerTest {

    private RoleManager roleManager;

    @Mock
    private AssignmentManager assignmentManager;

    private Role adminRole;
    private Role userRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager(assignmentManager);

        adminRole = new Role("ADMIN", "Administrator role");
        userRole = new Role("USER", "Regular user role");

        readPermission = new Permission("READ", "document", "Can read documents");
        writePermission = new Permission("WRITE", "document", "Can write documents");

        roleManager.add(adminRole);
        roleManager.add(userRole);
    }

    @Test
    void add_ShouldAddRole_WhenRoleIsValid() {
        Role newRole = new Role("GUEST", "Guest role");

        roleManager.add(newRole);

        assertEquals(3, roleManager.count());
        assertTrue(roleManager.exists("GUEST"));
        assertTrue(roleManager.findByName("GUEST").isPresent());
    }

    @Test
    void add_ShouldThrowException_WhenRoleIsNull() {
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(null));
    }

    @Test
    void add_ShouldThrowException_WhenNameAlreadyExists() {
        Role duplicateRole = new Role("ADMIN", "Duplicate admin");
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(duplicateRole));
    }

    @Test
    void remove_ShouldRemoveRole_WhenNoAssignments() {
        when(assignmentManager.findByRole(userRole)).thenReturn(List.of());

        boolean result = roleManager.remove(userRole);

        assertTrue(result);
        assertEquals(1, roleManager.count());
        assertFalse(roleManager.exists("USER"));
        verify(assignmentManager, times(1)).findByRole(userRole);
    }

    @Test
    void remove_ShouldThrowException_WhenRoleHasAssignments() {
        RoleAssignment mockAssignment = mock(RoleAssignment.class);
        when(assignmentManager.findByRole(adminRole)).thenReturn(List.of(mockAssignment));

        assertThrows(IllegalStateException.class, () -> roleManager.remove(adminRole));
        assertTrue(roleManager.exists("ADMIN"));
        verify(assignmentManager, times(1)).findByRole(adminRole);
    }

    @Test
    void remove_ShouldReturnFalse_WhenRoleIsNull() {
        assertFalse(roleManager.remove(null));
        verify(assignmentManager, never()).findByRole(any());
    }

    @Test
    void remove_ShouldReturnFalse_WhenRoleNotExists() {
        Role nonExistentRole = new Role("FAKE", "Fake role");

        assertFalse(roleManager.remove(nonExistentRole));
        verify(assignmentManager, never()).findByRole(any());
    }

    @Test
    void findById_ShouldReturnRole_WhenExists() {
        Optional<Role> found = roleManager.findById(adminRole.getId());

        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().name());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        assertFalse(roleManager.findById("nonexistent-id").isPresent());
    }

    @Test
    void findAll_ShouldReturnAllRoles() {
        List<Role> allRoles = roleManager.findAll();

        assertEquals(2, allRoles.size());
        assertTrue(allRoles.contains(adminRole));
        assertTrue(allRoles.contains(userRole));
    }

    @Test
    void count_ShouldReturnCorrectNumber() {
        assertEquals(2, roleManager.count());

        roleManager.add(new Role("GUEST", "Guest role"));
        assertEquals(3, roleManager.count());
    }

    @Test
    void clear_ShouldClearRoles_WhenNoAssignments() {
        when(assignmentManager.findByRole(any(Role.class))).thenReturn(List.of());

        roleManager.clear();

        assertEquals(0, roleManager.count());
        verify(assignmentManager, times(2)).findByRole(any(Role.class));
    }

    @Test
    void clear_ShouldThrowException_WhenAnyRoleHasAssignments() {
        RoleAssignment mockAssignment = mock(RoleAssignment.class);
        when(assignmentManager.findByRole(adminRole)).thenReturn(List.of(mockAssignment));
        when(assignmentManager.findByRole(userRole)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> roleManager.clear());
        assertEquals(2, roleManager.count());
        verify(assignmentManager, times(1)).findByRole(adminRole);
        verify(assignmentManager, times(1)).findByRole(userRole);
    }

    @Test
    void findByName_ShouldReturnRole_WhenExists() {
        Optional<Role> found = roleManager.findByName("ADMIN");

        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().name());
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        assertFalse(roleManager.findByName("NONEXISTENT").isPresent());
    }

    @Test
    void findByFilter_ShouldReturnFilteredRoles() {
        RoleFilter filter = role -> role.name().contains("ADMIN");

        List<Role> result = roleManager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).name());
    }

    @Test
    void findByFilter_ShouldReturnAllRoles_WhenFilterIsNull() {
        List<Role> result = roleManager.findByFilter(null);

        assertEquals(2, result.size());
    }

    @Test
    void findAll_WithFilterAndSorter_ShouldReturnFilteredAndSorted() {
        RoleFilter filter = role -> true;
        Comparator<Role> sorter = Comparator.comparing(Role::name);

        List<Role> result = roleManager.findAll(filter, sorter);

        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).name());
        assertEquals("USER", result.get(1).name());
    }

    @Test
    void exists_ShouldReturnTrue_WhenRoleExists() {
        assertTrue(roleManager.exists("ADMIN"));
    }

    @Test
    void exists_ShouldReturnFalse_WhenRoleDoesNotExist() {
        assertFalse(roleManager.exists("NONEXISTENT"));
    }

    @Test
    void addPermissionToRole_ShouldAddPermission_WhenRoleExists() {
        roleManager.addPermissionToRole("ADMIN", readPermission);

        assertTrue(adminRole.hasPermission(readPermission));

        List<Role> rolesWithRead = roleManager.findRolesWithPermission("READ", "document");
        assertTrue(rolesWithRead.contains(adminRole));
    }

    @Test
    void addPermissionToRole_ShouldThrowException_WhenRoleNotExists() {
        assertThrows(IllegalArgumentException.class,
                () -> roleManager.addPermissionToRole("NONEXISTENT", readPermission));
    }

    @Test
    void addPermissionToRole_WithNullPermission_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> roleManager.addPermissionToRole("ADMIN", null));
    }

    @Test
    void removePermissionFromRole_ShouldRemovePermission_WhenRoleExists() {
        roleManager.addPermissionToRole("ADMIN", readPermission);
        assertTrue(adminRole.hasPermission(readPermission));

        roleManager.removePermissionFromRole("ADMIN", readPermission);

        assertFalse(adminRole.hasPermission(readPermission));
    }

    @Test
    void findRolesWithPermission_ShouldReturnRolesWithPermission() {
        roleManager.addPermissionToRole("ADMIN", readPermission);
        roleManager.addPermissionToRole("USER", readPermission);

        List<Role> rolesWithRead = roleManager.findRolesWithPermission("READ", "document");

        assertEquals(2, rolesWithRead.size());
        assertTrue(rolesWithRead.contains(adminRole));
        assertTrue(rolesWithRead.contains(userRole));
    }

    @Test
    void findRolesWithPermission_ShouldReturnEmptyList_WhenNoRolesHavePermission() {
        List<Role> rolesWithWrite = roleManager.findRolesWithPermission("WRITE", "document");

        assertTrue(rolesWithWrite.isEmpty());
    }
}