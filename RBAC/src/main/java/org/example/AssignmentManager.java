package org.example;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssignmentManager implements Repository<RoleAssignment>{

    private final Map<String, RoleAssignment> roleAssignmentByAssignments;
    private final UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.roleAssignmentByAssignments = new HashMap<>();
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) {
            throw new IllegalArgumentException("Assignment cant be null");
        }

        User user = item.user();
        if (!userManager.exists(user.username())) {
            throw new IllegalArgumentException(
                    "User '" + user.username() + "' does not exist"
            );
        }

        Role role = item.role();
        if (!roleManager.exists(role.name())) {
            throw new IllegalArgumentException(
                    "Role '" + role.name() + "' does not exist"
            );
        }

        boolean hasActiveDuplicate = roleAssignmentByAssignments.values().stream()
                .filter(a -> a.user().equals(user))
                .filter(a -> a.role().equals(role))
                .anyMatch(RoleAssignment::isActive);

        if (hasActiveDuplicate) {
            throw new IllegalStateException(
                    "User '" + user.username() + "' already has active assignment for role '" +
                            role.name() + "'"
            );
        }

        roleAssignmentByAssignments.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) {
            return false;
        }
        return roleAssignmentByAssignments.remove(item.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(roleAssignmentByAssignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(roleAssignmentByAssignments.values());
    }

    @Override
    public int count() {
        return roleAssignmentByAssignments.size();
    }

    @Override
    public void clear() {
        roleAssignmentByAssignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return new ArrayList<>();
        }

        return roleAssignmentByAssignments.values().stream().filter(a -> a.user().equals(user)).collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return new ArrayList<>();
        }

        return roleAssignmentByAssignments.values().stream().filter(a -> a.role().equals(role)).collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return new ArrayList<>(roleAssignmentByAssignments.values());
        }

        return roleAssignmentByAssignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Stream<RoleAssignment> stream = roleAssignmentByAssignments.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return roleAssignmentByAssignments.values().stream().filter(RoleAssignment::isActive).collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return roleAssignmentByAssignments.values().stream().filter(a -> !a.isActive()).collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        return roleAssignmentByAssignments.values().stream().filter(a -> a.user().equals(user)).filter(a -> a.role().equals(role)).anyMatch(RoleAssignment::isActive);
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }

        return getUserPermissions(user).stream()
                .anyMatch(p -> p.name().equals(permissionName) && p.resource().equals(resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return new HashSet<>();
        }

        return roleAssignmentByAssignments.values().stream().filter(a -> a.user().equals(user)).filter(RoleAssignment::isActive)
                .map(RoleAssignment::role).flatMap(role -> role.getPermissions().stream()).collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        if (assignment instanceof PermanentAssignment perm) {
            perm.revoke();
        } else if (assignment instanceof TemporaryAssignment temp) {
            String yesterday = java.time.LocalDate.now().minusDays(1).toString();
            temp.extend(yesterday);
            System.out.println("Temporary assignment " + assignmentId + " expired as of " + yesterday);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        if (!(assignment instanceof TemporaryAssignment temp)) {
            throw new IllegalArgumentException("Assignment is not temporary");
        }

        temp.extend(newExpirationDate);
    }


    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }
}
