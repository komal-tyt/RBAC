package org.example;

public interface RoleAssignment {

    public String assignmentId();
    public User user();
    public Role role();
    public AssignmentMetadata metadata();
    public boolean isActive();
    public String assignmentType();

}
