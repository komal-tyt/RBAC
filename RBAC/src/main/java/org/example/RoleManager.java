package org.example;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById;
    private final Map<String, Role> rolesByName;
    private AssignmentManager assignmentManager;

    public RoleManager(AssignmentManager assignmentManager){
        this.rolesById = new HashMap<>();
        this.rolesByName = new HashMap<>();
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role item){
        if (item == null) {
            throw new IllegalArgumentException("Error! Role cant null");
        }

        if (rolesByName.containsKey(item.name())) {
            throw new IllegalArgumentException(
                    "Role with name '" + item.name() + "' already exists"
            );
        }

        rolesById.put(item.getId(), item);
        rolesByName.put(item.name(), item);
    }

    @Override
    public boolean remove(Role item){
        if (item == null){
            return false;
        }

        List<RoleAssignment> assignments = assignmentManager.findByRole(item);

        if (!assignments.isEmpty()) {
            throw new IllegalStateException("Cant delete role that is assigned to users");
        }

        Role removed = rolesById.remove(item.getId());
        if (removed != null) {
            rolesByName.remove(item.name());
            return true;
        }
        return false;
    }

    @Override
    public Optional<Role> findById(String id){
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        List<String> usedRoles = new ArrayList<>();

        for (Role role : rolesById.values()) {

            List<RoleAssignment> assignments = assignmentManager.findByRole(role);

            if (!assignments.isEmpty()) {
                usedRoles.add(role.name() + " (" + assignments.size() + " assignments)");
            }
        }

        if (!usedRoles.isEmpty()) {
            throw new IllegalStateException(
                    "Cant clear roles. Following roles are in use:\n  - " +
                            String.join("\n  - ", usedRoles)
            );
        }

        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name){
        if (name == null || name.isBlank()){
            return Optional.empty();
        }

        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter){
        if (filter == null){
            return new ArrayList<>(rolesById.values());
        }

        return rolesById.values().stream().filter(filter :: test).collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter){

        Stream<Role> stream = rolesById.values().stream();

        if (filter != null){
            stream = stream.filter(filter :: test);
        }

        if (sorter != null){
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String name){
        if (name == null){
            return false;
        }

        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream().filter(role -> role.hasPermission(permissionName, resource)).collect(Collectors.toList());
    }


}
