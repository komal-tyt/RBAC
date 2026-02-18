package org.example;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment{

    private String assignmentId;
    private User user;
    private Role role;
    private AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata){

        this.assignmentId = generateId();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    public String generateId(){
        return "role_" + UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractRoleAssignment abstractroleassignment)) return false;
        return this.assignmentId.equals(abstractroleassignment.assignmentId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(assignmentId);
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }


    public abstract boolean isActive();
    public abstract String assignmentType();

    public String summary(){
        String result = "[" + assignmentType() + "] "
                + role.name() + " assigned to " + user.username() + " by " +
                metadata.assignedBy() + " at " + metadata.assignedAt() + "\n";

        if (metadata.reason() != null && !metadata.reason().isBlank()) {
            result += "Reason: " + metadata.reason() + "\n";
        } else {
            result += "Reason: No reason provided\n";
        }

        result += "Status: " + (isActive() ? "ACTIVE" : "INACTIVE");

        return result;

    }

}
