package org.example;

public class PermanentAssignment extends AbstractRoleAssignment{

    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata){
        super(user, role, metadata);
        this.revoked = false;

    }

    public boolean isActive() {
        return !revoked;
    }

    public String assignmentType(){
        return "PERMANENT";
    }

    public void revoke(){
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

}
