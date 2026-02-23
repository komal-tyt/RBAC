package org.example;

import java.util.Comparator;

public class AssignmentSorters {

    public static Comparator<RoleAssignment> byUsername(){
        return Comparator.comparing(a -> a.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName(){
        return Comparator.comparing(a -> a.role().name());
    }

    public static Comparator<RoleAssignment> byAssignmentDate(){
        return Comparator.comparing(a -> a.metadata().assignedAt());
    }
}
