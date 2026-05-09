package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> roleAssignmentByAssignments;
    private final ReentrantReadWriteLock lock;
    private final UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.roleAssignmentByAssignments = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
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

        lock.readLock().lock();
        try {
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
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            roleAssignmentByAssignments.put(item.assignmentId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            return roleAssignmentByAssignments.remove(item.assignmentId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(roleAssignmentByAssignments.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(roleAssignmentByAssignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            roleAssignmentByAssignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(a -> a.user().equals(user))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(a -> a.role().equals(role))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            Stream<RoleAssignment> stream = roleAssignmentByAssignments.values().stream();

            if (filter != null) {
                stream = stream.filter(filter::test);
            }

            if (sorter != null) {
                stream = stream.sorted(sorter);
            }

            return stream.collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(a -> !a.isActive())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(a -> a.user().equals(user))
                    .filter(a -> a.role().equals(role))
                    .anyMatch(RoleAssignment::isActive);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return getUserPermissions(user).stream()
                    .anyMatch(p -> p.name().equals(permissionName) && p.resource().equals(resource));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return new HashSet<>();
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().stream()
                    .filter(a -> a.user().equals(user))
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::role)
                    .flatMap(role -> role.getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = findById(assignmentId).orElseThrow(() ->
                    new IllegalArgumentException("Assignment not found: " + assignmentId));

            if (assignment instanceof PermanentAssignment perm) {
                perm.revoke();
            } else if (assignment instanceof TemporaryAssignment temp) {
                String yesterday = java.time.LocalDate.now().minusDays(1).toString();
                temp.extend(yesterday);
                System.out.println("Temporary assignment " + assignmentId + " expired as of " + yesterday);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = findById(assignmentId).orElseThrow(() ->
                    new IllegalArgumentException("Assignment not found: " + assignmentId));

            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalArgumentException("Assignment is not temporary");
            }

            temp.extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return roleAssignmentByAssignments.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}