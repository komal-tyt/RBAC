package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById;
    private final Map<String, Role> rolesByName;
    private final ReentrantReadWriteLock lock;
    private AssignmentManager assignmentManager;

    public RoleManager(AssignmentManager assignmentManager) {
        this.rolesById = new ConcurrentHashMap<>();
        this.rolesByName = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role item) {
        if (item == null) {
            throw new IllegalArgumentException("Error! Role cannot be null");
        }

        lock.writeLock().lock();
        try {
            if (rolesByName.containsKey(item.name())) {
                throw new IllegalArgumentException(
                        "Role with name '" + item.name() + "' already exists"
                );
            }
            rolesById.put(item.getId(), item);
            rolesByName.put(item.name(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Role item) {
        if (item == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            List<RoleAssignment> assignments = assignmentManager.findByRole(item);

            if (!assignments.isEmpty()) {
                throw new IllegalStateException("Cannot delete role that is assigned to users");
            }

            Role removed = rolesById.remove(item.getId());
            if (removed != null) {
                rolesByName.remove(item.name());
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rolesById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return rolesById.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            List<String> usedRoles = new ArrayList<>();

            for (Role role : rolesById.values()) {
                List<RoleAssignment> assignments = assignmentManager.findByRole(role);
                if (!assignments.isEmpty()) {
                    usedRoles.add(role.name() + " (" + assignments.size() + " assignments)");
                }
            }

            if (!usedRoles.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot clear roles. Following roles are in use:\n  - " +
                                String.join("\n  - ", usedRoles)
                );
            }

            rolesById.clear();
            rolesByName.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesByName.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            Stream<Role> stream = rolesById.values().stream();

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

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return rolesByName.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            Role role = findByName(roleName).orElseThrow(() ->
                    new IllegalArgumentException("Role not found: " + roleName));
            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            Role role = findByName(roleName).orElseThrow(() ->
                    new IllegalArgumentException("Role not found: " + roleName));
            role.removePermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(role -> role.hasPermission(permissionName, resource))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return rolesById.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}