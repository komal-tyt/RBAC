package org.example;

import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class Role {

    private final String id;
    private String name;
    private String description;
    private final Set<Permission> permissions;

    public Role(String name, String description){
        this.id = generateId();
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public String generateId(){
        return "role_" + UUID.randomUUID().toString();
    }

    public void addPermission(Permission permission){
        if (permission == null){
            throw new IllegalArgumentException("Permission is null!");
        }
        permissions.add(permission);
    }

    public void removePermission(Permission permission){
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission){
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource){
        for (Permission p: permissions){
            if (p.name().equals(permissionName) && p.resource().equals(resource)){
                return true;
            }
        }
        return false;
    }

    public Set<Permission> getPermissions(){
        return Set.copyOf(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role role)) return false;
        return this.id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // число на основе ID
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                '}';
    }

    public String format(){
        String result = "Role: " + this.name + "[ID: " + this.id + "]\n"
                + "Description: " + this.description +
                "\nPermissions: (" + permissions.size() + ")\n";
        for (Permission p : permissions){
            result += "- " + p.format() + "\n";
        }
        return result;

    }

}
