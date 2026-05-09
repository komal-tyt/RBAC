package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(80)).append("\n");
        report.append("USER REPORT\n");
        report.append("=".repeat(80)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("No users found in the system.\n");
            return report.toString();
        }

        for (User user : users) {
            report.append("User: ").append(user.username()).append("\n");
            report.append("  Full Name: ").append(user.fullName()).append("\n");
            report.append("  Email: ").append(user.email()).append("\n");

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            List<RoleAssignment> activeAssignments = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            report.append("  Roles (").append(activeAssignments.size()).append(" active):\n");

            if (activeAssignments.isEmpty()) {
                report.append("    No active roles assigned\n");
            } else {
                for (RoleAssignment ra : activeAssignments) {
                    report.append("    - ").append(ra.role().name())
                            .append(" (").append(ra.assignmentType()).append(")\n");
                    if (ra.metadata().reason() != null && !ra.metadata().reason().isBlank()) {
                        report.append("      Reason: ").append(ra.metadata().reason()).append("\n");
                    }
                }
            }
            report.append("\n");
        }

        report.append("-".repeat(80)).append("\n");
        report.append("Total users: ").append(users.size()).append("\n");
        report.append("=".repeat(80)).append("\n");

        return report.toString();
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(80)).append("\n");
        report.append("USER REPORT (PARALLEL)\n");
        report.append("=".repeat(80)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("No users found in the system.\n");
            return report.toString();
        }

        List<String> userReports = users.parallelStream()
                .map(user -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("User: ").append(user.username()).append("\n");
                    sb.append("  Full Name: ").append(user.fullName()).append("\n");
                    sb.append("  Email: ").append(user.email()).append("\n");

                    List<RoleAssignment> assignments = assignmentManager.findByUser(user);
                    List<RoleAssignment> activeAssignments = assignments.stream()
                            .filter(RoleAssignment::isActive)
                            .collect(Collectors.toList());

                    sb.append("  Roles (").append(activeAssignments.size()).append(" active):\n");

                    if (activeAssignments.isEmpty()) {
                        sb.append("    No active roles assigned\n");
                    } else {
                        for (RoleAssignment ra : activeAssignments) {
                            sb.append("    - ").append(ra.role().name())
                                    .append(" (").append(ra.assignmentType()).append(")\n");
                            if (ra.metadata().reason() != null && !ra.metadata().reason().isBlank()) {
                                sb.append("      Reason: ").append(ra.metadata().reason()).append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                    return sb.toString();
                })
                .collect(Collectors.toList());

        for (String userReport : userReports) {
            report.append(userReport);
        }

        report.append("-".repeat(80)).append("\n");
        report.append("Total users: ").append(users.size()).append("\n");
        report.append("=".repeat(80)).append("\n");

        return report.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(80)).append("\n");
        report.append("ROLE REPORT\n");
        report.append("=".repeat(80)).append("\n\n");

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            report.append("No roles found in the system.\n");
            return report.toString();
        }

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeAssignments = assignments.stream().filter(RoleAssignment::isActive).count();

            report.append("Role: ").append(role.name()).append("\n");
            report.append("  ID: ").append(role.getId()).append("\n");
            report.append("  Description: ").append(role.getDescription()).append("\n");
            report.append("  Permissions: ").append(role.getPermissions().size()).append("\n");
            report.append("  Users with this role: ").append(activeAssignments).append("\n");

            if (activeAssignments > 0) {
                report.append("  Users:\n");
                for (RoleAssignment ra : assignments) {
                    if (ra.isActive()) {
                        report.append("    - ").append(ra.user().username())
                                .append(" (").append(ra.user().fullName()).append(")\n");
                    }
                }
            }
            report.append("\n");
        }

        report.append("-".repeat(80)).append("\n");
        report.append("Total roles: ").append(roles.size()).append("\n");
        report.append("=".repeat(80)).append("\n");

        return report.toString();
    }

    public String generateRoleReportParallel(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("=".repeat(80)).append("\n");
        report.append("ROLE REPORT (PARALLEL)\n");
        report.append("=".repeat(80)).append("\n\n");

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            report.append("No roles found in the system.\n");
            return report.toString();
        }

        List<String> roleReports = roles.parallelStream()
                .map(role -> {
                    StringBuilder sb = new StringBuilder();
                    List<RoleAssignment> assignments = assignmentManager.findByRole(role);
                    long activeAssignments = assignments.stream().filter(RoleAssignment::isActive).count();

                    sb.append("Role: ").append(role.name()).append("\n");
                    sb.append("  ID: ").append(role.getId()).append("\n");
                    sb.append("  Description: ").append(role.getDescription()).append("\n");
                    sb.append("  Permissions: ").append(role.getPermissions().size()).append("\n");
                    sb.append("  Users with this role: ").append(activeAssignments).append("\n");

                    if (activeAssignments > 0) {
                        sb.append("  Users:\n");
                        for (RoleAssignment ra : assignments) {
                            if (ra.isActive()) {
                                sb.append("    - ").append(ra.user().username())
                                        .append(" (").append(ra.user().fullName()).append(")\n");
                            }
                        }
                    }
                    sb.append("\n");
                    return sb.toString();
                })
                .collect(Collectors.toList());

        for (String roleReport : roleReports) {
            report.append(roleReport);
        }

        report.append("-".repeat(80)).append("\n");
        report.append("Total roles: ").append(roles.size()).append("\n");
        report.append("=".repeat(80)).append("\n");

        return report.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder matrix = new StringBuilder();

        matrix.append("=".repeat(100)).append("\n");
        matrix.append("PERMISSION MATRIX (Users × Resources)\n");
        matrix.append("=".repeat(100)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            matrix.append("No users found in the system.\n");
            return matrix.toString();
        }

        Set<String> allResources = new TreeSet<>();
        for (User user : users) {
            for (Permission p : assignmentManager.getUserPermissions(user)) {
                allResources.add(p.resource());
            }
        }

        if (allResources.isEmpty()) {
            matrix.append("No permissions assigned to any user.\n");
            return matrix.toString();
        }

        List<String> resources = new ArrayList<>(allResources);

        matrix.append(String.format("%-15s", "USERNAME"));
        for (String resource : resources) {
            matrix.append(String.format(" | %-15s", resource));
        }
        matrix.append("\n");

        matrix.append("-".repeat(15 + resources.size() * 19)).append("\n");

        for (User user : users) {
            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
            Map<String, Set<String>> userResources = new HashMap<>();

            for (Permission p : userPerms) {
                userResources.computeIfAbsent(p.resource(), k -> new TreeSet<>()).add(p.name());
            }

            matrix.append(String.format("%-15s", user.username()));

            for (String resource : resources) {
                Set<String> actions = userResources.getOrDefault(resource, new TreeSet<>());
                String display = actions.isEmpty() ? "-" : String.join(",", actions);
                if (display.length() > 15) {
                    display = display.substring(0, 12) + "...";
                }
                matrix.append(String.format(" | %-15s", display));
            }
            matrix.append("\n");
        }

        matrix.append("-".repeat(100)).append("\n");
        matrix.append("Resources: ").append(String.join(", ", resources)).append("\n");
        matrix.append("=".repeat(100)).append("\n");

        return matrix.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder matrix = new StringBuilder();

        matrix.append("=".repeat(100)).append("\n");
        matrix.append("PERMISSION MATRIX (PARALLEL)\n");
        matrix.append("=".repeat(100)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            matrix.append("No users found in the system.\n");
            return matrix.toString();
        }

        Set<String> allResources = users.parallelStream()
                .flatMap(user -> assignmentManager.getUserPermissions(user).stream())
                .map(Permission::resource)
                .collect(Collectors.toSet());

        if (allResources.isEmpty()) {
            matrix.append("No permissions assigned to any user.\n");
            return matrix.toString();
        }

        List<String> resources = new ArrayList<>(allResources);
        Collections.sort(resources);

        matrix.append(String.format("%-15s", "USERNAME"));
        for (String resource : resources) {
            matrix.append(String.format(" | %-15s", resource));
        }
        matrix.append("\n");

        matrix.append("-".repeat(15 + resources.size() * 19)).append("\n");

        List<String> userRows = users.parallelStream()
                .map(user -> {
                    Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
                    Map<String, Set<String>> userResources = new HashMap<>();

                    for (Permission p : userPerms) {
                        userResources.computeIfAbsent(p.resource(), k -> new TreeSet<>()).add(p.name());
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%-15s", user.username()));

                    for (String resource : resources) {
                        Set<String> actions = userResources.getOrDefault(resource, new TreeSet<>());
                        String display = actions.isEmpty() ? "-" : String.join(",", actions);
                        if (display.length() > 15) {
                            display = display.substring(0, 12) + "...";
                        }
                        sb.append(String.format(" | %-15s", display));
                    }
                    sb.append("\n");
                    return sb.toString();
                })
                .collect(Collectors.toList());

        for (String row : userRows) {
            matrix.append(row);
        }

        matrix.append("-".repeat(100)).append("\n");
        matrix.append("Resources: ").append(String.join(", ", resources)).append("\n");
        matrix.append("=".repeat(100)).append("\n");

        return matrix.toString();
    }

    public void exportToFile(String report, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            filename = "report.txt";
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
            System.out.println("Report saved to '" + filename + "' successfully!");
        } catch (IOException e) {
            System.out.println("Error saving report: " + e.getMessage());
        }
    }
}