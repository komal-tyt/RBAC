package org.example;


import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class CommandRegistry {

    private final CommandParser parser;
    private final RBACSystem system;
    private final Scanner scanner;

    public CommandRegistry(CommandParser parser, RBACSystem system, Scanner scanner) {
        this.parser = parser;
        this.system = system;
        this.scanner = scanner;
    }

    private void registerUserCommands() {
        //Команды управления пользователями:

        parser.registerCommand("user-list", "List all users", (s, sys) -> {
            System.out.println("\n=== USER LIST ===");

            List<User> users = sys.getUserManager().findAll();

            System.out.print("Filter (Enter for all): ");
            String filter = s.nextLine().trim();

            if (!filter.isEmpty()) {
                users = users.stream().filter(u -> u.username().contains(filter) || u.email().contains(filter)).toList();
            }

            if (users.isEmpty()) {
                System.out.println("No users found.");
                return;
            }

            System.out.println("\n" + "-".repeat(70));
            System.out.printf("| %-15s | %-25s | %-20s |\n", "USERNAME", "FULL NAME", "EMAIL");
            System.out.println("-".repeat(70));

            for (User user : users) {
                System.out.printf("| %-15s | %-25s | %-20s |\n",
                        user.username(),
                        user.fullName().length() > 25 ? user.fullName().substring(0, 22) + "..." : user.fullName(),
                        user.email().length() > 20 ? user.email().substring(0, 17) + "..." : user.email()
                );
            }

            System.out.println("-".repeat(70));
            System.out.println("| Total: " + users.size() + " users" + " ".repeat(50 - String.valueOf(users.size()).length() - 14) + "|");
            System.out.println("-".repeat(70));
        });


        parser.registerCommand("user-create", "Create new user", (s, sys) -> {
            System.out.println("\n=== CREATE NEW USER ===");

            try {
                System.out.println("Username: ");
                String username = s.nextLine().trim();

                System.out.print("Full Name: ");
                String fullName = s.nextLine().trim();

                System.out.print("Email: ");
                String email = s.nextLine().trim();

                User user = User.create(username, fullName, email);

                sys.getUserManager().add(user);

                System.out.println("\nUser created successfully!");
                System.out.println("  Username: " + user.username());
                System.out.println("  Full Name: " + user.fullName());
                System.out.println("  Email: " + user.email());

            } catch (IllegalArgumentException e) {
                System.out.println("\nError: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\nError creating user: " + e.getMessage());
            }
        });


        parser.registerCommand("user-view", "View user", (s, sys) -> {
            System.out.println("\n=== VIEW USER ===");

            System.out.println("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                System.out.println("\nUSER INFORMATION");
                System.out.println("----------------------------------------");
                System.out.println("Username: " + user.username());
                System.out.println("Full Name: " + user.fullName());
                System.out.println("Email: " + user.email());

                List<RoleAssignment> assignments = am.findByUser(user);
                System.out.println("\nASSIGNED ROLES (" + assignments.size() + ")");
                System.out.println("----------------------------------------");

                if (assignments.isEmpty()) {
                    System.out.println("No roles assigned");
                } else {
                    for (RoleAssignment ra : assignments) {
                        String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                        System.out.println("  - " + ra.role().name() + " (" + ra.assignmentType() + ") - " + status);
                        System.out.println("    Assigned by: " + ra.metadata().assignedBy() + " at " + ra.metadata().assignedAt());
                        if (ra.metadata().reason() != null && !ra.metadata().reason().isBlank()) {
                            System.out.println("    Reason: " + ra.metadata().reason());
                        }
                    }
                }

                var permissions = am.getUserPermissions(user);
                System.out.println("\nALL PERMISSIONS (" + permissions.size() + ")");
                System.out.println("----------------------------------------");

                if (permissions.isEmpty()) {
                    System.out.println("No permissions");
                } else {
                    for (Permission p : permissions) {
                        System.out.println("  - " + p.name() + " on " + p.resource() + ": " + p.description());
                    }
                }

            } else {
                System.out.println("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-update", "Update user information", (s, sys) -> {
            System.out.println("\n=== USER UPDATE ===");

            System.out.println("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()){
                User user = optionalUser.get();

                System.out.println("\nCurrent information:");
                System.out.println("  Full Name: " + user.fullName());
                System.out.println("  Email: " + user.email());

                System.out.println("\nEnter the new Full name: ");
                String newFullName = s.nextLine().trim();
                if (newFullName.isEmpty()) {
                    newFullName = user.fullName();
                }

                System.out.println("\nEnter the new Email: ");
                String newEmail = s.nextLine().trim();
                if (newEmail.isEmpty()) {
                    newEmail = user.email();
                }

                try {
                    um.update(username, newFullName, newEmail);
                    System.out.println("\nUser updated successfully!");

                    User updated = um.findByUsername(username).get();
                    System.out.println("  Username: " + updated.username());
                    System.out.println("  Full Name: " + updated.fullName());
                    System.out.println("  Email: " + updated.email());
                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else {
                System.out.println("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-delete", "Delete User", (s, sys) -> {
            System.out.println("\n=== USER DELETE ===");

            System.out.println("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()){
                User user = optionalUser.get();

                System.out.println("\nAre you sure to delete?(y/n)\n");
                String answer = s.nextLine().trim();

                if (answer.equals("y") || answer.equals("yes")){
                    List<RoleAssignment> assignments = am.findByUser(user);

                    for (RoleAssignment ra : assignments) {
                        am.remove(ra);
                    }

                    um.remove(user);
                    System.out.println("User '" + username + "' deleted successfully!");
                } else if (answer.equals("n") || answer.equals("no")){
                    System.out.println("Deletion cancelled");
                } else {
                    System.out.println("Answer not correct");
                }
            } else{
                System.out.println("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-search","Search user with filters", (s, sys) -> {
            System.out.println("\n=== Search Users ===");
            System.out.println("Select filter:");
            System.out.println("  1. By username (contains)");
            System.out.println("  2. By email (contains)");
            System.out.println("  3. By email domain");
            System.out.println("  4. By full name (contains)");
            System.out.print("Choice (1-4): ");

            String choice = s.nextLine().trim();

            List<User> allUsers = sys.getUserManager().findAll();
            List<User> results = new java.util.ArrayList<>();
            String filterDesc = "";

            if (choice.equals("1")) {
                System.out.print("Enter username part: ");
                String part = s.nextLine().trim().toLowerCase();
                results = allUsers.stream()
                        .filter(u -> u.username().toLowerCase().contains(part))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "username contains '" + part + "'";

            } else if (choice.equals("2")) {
                System.out.print("Enter email part: ");
                String part = s.nextLine().trim().toLowerCase();
                results = allUsers.stream()
                        .filter(u -> u.email().toLowerCase().contains(part))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "email contains '" + part + "'";

            } else if (choice.equals("3")) {
                System.out.print("Enter email domain: ");
                String domain = s.nextLine().trim().toLowerCase();
                results = allUsers.stream()
                        .filter(u -> u.email().toLowerCase().endsWith(domain))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "email domain '" + domain + "'";

            } else if (choice.equals("4")) {
                System.out.print("Enter full name part: ");
                String part = s.nextLine().trim().toLowerCase();
                results = allUsers.stream()
                        .filter(u -> u.fullName().toLowerCase().contains(part))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "full name contains '" + part + "'";

            } else {
                System.out.println("Invalid choice");
                return;
            }

            if (results.isEmpty()) {
                System.out.println("\nNo users found with filter: " + filterDesc);
            } else {
                System.out.println("\nSearch results for: " + filterDesc);
                System.out.println("\n----------------------------------------");

                for (User user : results) {
                    System.out.println("  " + user.username() + " | " + user.fullName() + " | " + user.email());
                }

                System.out.println("----------------------------------------");
                System.out.println("Total: " + results.size() + " users");
            }
        });

        //Команды управления ролями:

        parser.registerCommand("role-list", "List all roles", (s, sys) -> {
            System.out.println("\n=== ROLE LIST ===");

            List<Role> roles = sys.getRoleManager().findAll();

            if (roles.isEmpty()) {
                System.out.println("No roles found.");
                return;
            }

            System.out.println("\n----------------------------------------");

            for (Role role : roles) {
                System.out.println("  " + role.name() + " | " +
                        role.getPermissions().size() + " permissions | " +
                        role.getId());
            }

            System.out.println("----------------------------------------");
            System.out.println("Total: " + roles.size() + " roles");
        });

        parser.registerCommand("role-create", "Create new role", (s, sys) -> {
            System.out.println("\n=== CREATE NEW ROLE ===");

            try{
                System.out.println("Role: ");
                String name = s.nextLine().trim();

                System.out.print("Role description: ");
                String description = s.nextLine().trim();

                Role role = new Role(name, description);
                sys.getRoleManager().add(role);

                System.out.println("\nRole '" + name + "' created successfully!");
                System.out.println("  ID: " + role.getId());
                System.out.println("  Description: " + role.getDescription());

                System.out.println("\nDo you want to add permissions to this role? (y/n): ");
                String answer = s.nextLine().trim().toLowerCase();

                if (answer.equals("y") || answer.equals("yes")) {
                    boolean adding = true;

                    while (adding) {
                        System.out.println("\n--- Add Permission ---");
                        System.out.print("Permission name (e.g., READ, WRITE, DELETE): ");
                        String permName = s.nextLine().trim().toUpperCase();

                        System.out.print("Resource (e.g., users, reports, *): ");
                        String resource = s.nextLine().trim().toLowerCase();

                        System.out.print("Description: ");
                        String permDesc = s.nextLine().trim();

                        try {
                            Permission permission = new Permission(permName, resource, permDesc);
                            role.addPermission(permission);
                            System.out.println("Permission added successfully!");

                            System.out.print("\nAdd another permission? (y/n): ");
                            String again = s.nextLine().trim().toLowerCase();
                            if (!again.equals("y") && !again.equals("yes")) {
                                adding = false;
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }

                    System.out.println("\nRole '" + name + "' now has " + role.getPermissions().size() + " permissions.");
                }

            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error creating role: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role", (s, sys) -> {
            System.out.println("\n=== VIEW ROLE ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();
                System.out.println(role.format());
            } else {
                System.out.println("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-update", "Update role (name/description)", (s, sys) -> {
            System.out.println("\n=== UPDATE ROLE ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                System.out.println("\nCurrent information:");
                System.out.println("  Name: " + role.name());
                System.out.println("  Description: " + role.getDescription());

                System.out.println("\nEnter new data (press Enter to keep current):");

                System.out.print("New role name: ");
                String newName = s.nextLine().trim();
                if (newName.isEmpty()) {
                    newName = role.name();
                }

                System.out.print("New description: ");
                String newDescription = s.nextLine().trim();
                if (newDescription.isEmpty()) {
                    newDescription = role.getDescription();
                }

                try {

                    if (!newName.equals(role.name())) {
                        if (rm.findByName(newName).isPresent()) {
                            System.out.println("Error: Role with name '" + newName + "' already exists");
                            return;
                        }
                    }

                    Role updatedRole = new Role(newName, newDescription);

                    for (Permission p : role.getPermissions()) {
                        updatedRole.addPermission(p);
                    }

                    rm.remove(role);

                    rm.add(updatedRole);

                    System.out.println("\nRole updated successfully!");
                    System.out.println("  Name: " + updatedRole.name());
                    System.out.println("  Description: " + updatedRole.getDescription());
                    System.out.println("  Permissions: " + updatedRole.getPermissions().size());

                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Error updating role: " + e.getMessage());
                }

            } else {
                System.out.println("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-delete", "Delete role", (s, sys) -> {
            System.out.println("\n=== DELETE ROLE ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                List<RoleAssignment> assignments = am.findByRole(role);

                if (!assignments.isEmpty()) {
                    System.out.println("\nError: Role is assigned to " + assignments.size() + " user(s):");
                    for (RoleAssignment ra : assignments) {
                        System.out.println("  - " + ra.user().username() + " (" + ra.user().fullName() + ")");
                    }
                    System.out.println("\nDeleting this role will remove these assignments!");
                }

                System.out.print("\nAre you sure you want to delete role '" + roleName + "'? (yes/no): ");
                String answer = s.nextLine().trim().toLowerCase();

                if (answer.equals("yes") || answer.equals("y")) {
                    for (RoleAssignment ra : assignments) {
                        am.remove(ra);
                    }

                    rm.remove(role);
                    System.out.println("Role '" + roleName + "' deleted successfully!");
                } else if (answer.equals("no") || answer.equals("n")) {
                    System.out.println("Deletion cancelled");
                } else {
                    System.out.println("Answer not correct");
                }

            } else {
                System.out.println("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (s, sys) -> {
            System.out.println("\n=== ADD PERMISSION TO ROLE ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                System.out.println("\nCurrent permissions (" + role.getPermissions().size() + "):");
                for (Permission p : role.getPermissions()) {
                    System.out.println("  - " + p.name() + " on " + p.resource());
                }

                System.out.println("\n--- New Permission ---");
                System.out.print("Permission name (READ, WRITE, DELETE): ");
                String permName = s.nextLine().trim().toUpperCase();

                System.out.print("Resource (users, reports, *): ");
                String resource = s.nextLine().trim().toLowerCase();

                System.out.print("Description: ");
                String description = s.nextLine().trim();

                try {
                    Permission permission = new Permission(permName, resource, description);
                    role.addPermission(permission);
                    System.out.println("\nPermission added successfully!");
                    System.out.println("  " + permission.format());
                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                }

            } else {
                System.out.println("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (s, sys) -> {
            System.out.println("\n=== REMOVE PERMISSION FROM ROLE ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                List<Permission> permissions = role.getPermissions().stream().collect(java.util.stream.Collectors.toList());

                if (permissions.isEmpty()) {
                    System.out.println("Role has no permissions.");
                    return;
                }

                System.out.println("\nPermissions:");
                for (int i = 0; i < permissions.size(); i++) {
                    Permission p = permissions.get(i);
                    System.out.println("  " + (i + 1) + ". " + p.name() + " on " + p.resource());
                }

                System.out.print("\nEnter number to remove: ");
                String choice = s.nextLine().trim();

                try {
                    int index = Integer.parseInt(choice) - 1;
                    if (index >= 0 && index < permissions.size()) {
                        Permission toRemove = permissions.get(index);
                        role.removePermission(toRemove);
                        System.out.println("Permission removed: " + toRemove.name() + " on " + toRemove.resource());
                    } else {
                        System.out.println("Invalid number!");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input!");
                }

            } else {
                System.out.println("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-search", "Search roles", (s, sys) -> {
            System.out.println("\n=== SEARCH ROLES ===");
            System.out.println("Select filter:");
            System.out.println("  1. By name (contains)");
            System.out.println("  2. By permission (has specific permission)");
            System.out.println("  3. By minimum number of permissions");
            System.out.print("Choice (1-3): ");

            String choice = s.nextLine().trim();

            List<Role> allRoles = sys.getRoleManager().findAll();
            List<Role> results = new java.util.ArrayList<>();
            String filterDesc = "";

            if (choice.equals("1")) {
                System.out.print("Enter role name part: ");
                String part = s.nextLine().trim().toLowerCase();
                results = allRoles.stream()
                        .filter(r -> r.name().toLowerCase().contains(part))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "name contains '" + part + "'";

            } else if (choice.equals("2")) {
                System.out.print("Enter permission name (READ, WRITE, DELETE): ");
                String permName = s.nextLine().trim().toUpperCase();
                System.out.print("Enter resource (users, reports, *): ");
                String resource = s.nextLine().trim().toLowerCase();

                results = allRoles.stream()
                        .filter(r -> r.hasPermission(permName, resource))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "has permission " + permName + " on " + resource;

            } else if (choice.equals("3")) {
                System.out.print("Enter minimum number of permissions: ");
                try {
                    int min = Integer.parseInt(s.nextLine().trim());
                    results = allRoles.stream()
                            .filter(r -> r.getPermissions().size() >= min)
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "has at least " + min + " permissions";
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number!");
                    return;
                }

            } else {
                System.out.println("Invalid choice");
                return;
            }

            if (results.isEmpty()) {
                System.out.println("\nNo roles found with filter: " + filterDesc);
            } else {
                System.out.println("\nSearch results for: " + filterDesc);
                System.out.println("\n----------------------------------------");

                for (Role role : results) {
                    System.out.println("  - " + role.name() + " (" + role.getPermissions().size() + " permissions)");
                    System.out.println("    ID: " + role.getId());
                }

                System.out.println("----------------------------------------");
                System.out.println("Total: " + results.size() + " roles");
            }
        });

        //Команды управления назначениями:

        parser.registerCommand("assign-role", "Assign role to user", (s, sys) -> {
            System.out.println("\n=== ASSIGN ROLE ===");

            System.out.print("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<Role> roles = rm.findAll();
            if (roles.isEmpty()) {
                System.out.println("No roles available. Create a role first.");
                return;
            }

            System.out.println("\nAvailable roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + roles.get(i).name());
            }


            System.out.print("\nSelect role (number): ");
            int roleIndex;
            try {
                roleIndex = Integer.parseInt(s.nextLine().trim()) - 1;
                if (roleIndex < 0 || roleIndex >= roles.size()) {
                    System.out.println("Invalid selection");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
                return;
            }
            Role role = roles.get(roleIndex);

            System.out.print("\nAssignment type (permanent/temporary): ");
            String type = s.nextLine().trim().toLowerCase();

            System.out.print("Reason: ");
            String reason = s.nextLine().trim();

            AssignmentMetadata metadata = AssignmentMetadata.now(sys.getCurrentUser(), reason);

            try {
                if (type.equals("permanent") || type.equals("p")) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    am.add(assignment);
                    System.out.println("\nPermanent role assigned successfully!");
                    System.out.println("  User: " + username);
                    System.out.println("  Role: " + role.name());
                    System.out.println("  Reason: " + reason);

                } else if (type.equals("temporary") || type.equals("t")) {
                    System.out.print("Expiration date (YYYY-MM-DD): ");
                    String expiresAt = s.nextLine().trim();

                    System.out.print("Auto-renew? (y/n): ");
                    boolean autoRenew = s.nextLine().trim().toLowerCase().equals("y");

                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    am.add(assignment);
                    System.out.println("\nTemporary role assigned successfully!");
                    System.out.println("  User: " + username);
                    System.out.println("  Role: " + role.name());
                    System.out.println("  Expires: " + expiresAt);
                    System.out.println("  Auto-renew: " + (autoRenew ? "Yes" : "No"));
                    System.out.println("  Reason: " + reason);

                } else {
                    System.out.println("Invalid type. Use 'permanent' or 'temporary'");
                }

            } catch (IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error creating assignment: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (s, sys) -> {
            System.out.println("\n=== REVOKE ROLE ===");

            System.out.print("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> activeAssignments = am.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(java.util.stream.Collectors.toList());

            if (activeAssignments.isEmpty()) {
                System.out.println("No active assignments for user '" + username + "'");
                return;
            }

            System.out.println("\nActive assignments for " + username + ":");
            for (int i = 0; i < activeAssignments.size(); i++) {
                RoleAssignment ra = activeAssignments.get(i);
                String typeInfo = ra.assignmentType();
                if (ra instanceof TemporaryAssignment) {
                    typeInfo += " (expires: " + ((TemporaryAssignment) ra).getExpiresAt() + ")";
                }
                System.out.println("  " + (i + 1) + ". " + ra.role().name() + " - " + typeInfo);
            }

            System.out.print("\nSelect assignment to revoke (number): ");
            int choice;
            try {
                choice = Integer.parseInt(s.nextLine().trim()) - 1;
                if (choice < 0 || choice >= activeAssignments.size()) {
                    System.out.println("Invalid selection");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
                return;
            }

            RoleAssignment toRevoke = activeAssignments.get(choice);

            if (toRevoke instanceof PermanentAssignment) {
                ((PermanentAssignment) toRevoke).revoke();
                System.out.println("\nPermanent assignment revoked successfully!");
                System.out.println("  User: " + username);
                System.out.println("  Role: " + toRevoke.role().name());

            } else if (toRevoke instanceof TemporaryAssignment) {
                String yesterday = java.time.LocalDate.now().minusDays(1).toString();
                ((TemporaryAssignment) toRevoke).extend(yesterday);
                System.out.println("\nTemporary assignment expired successfully!");
                System.out.println("  User: " + username);
                System.out.println("  Role: " + toRevoke.role().name());
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (s, sys) -> {
            System.out.println("\n=== ASSIGNMENT LIST ===");

            List<RoleAssignment> assignments = sys.getAssignmentManager().findAll();

            if (assignments.isEmpty()) {
                System.out.println("No assignments found.");
                return;
            }

            System.out.println("\n----------------------------------------");

            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.println("  " + ra.user().username() + " | " +
                        ra.role().name() + " | " +
                        ra.assignmentType() + " | " +
                        status + " | " +
                        ra.metadata().assignedAt());
            }

            System.out.println("----------------------------------------");
            System.out.println("Total: " + assignments.size() + " assignments");
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (s, sys) -> {
            System.out.println("\n=== LIST USER ASSIGNMENTS ===");

            System.out.print("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> assignments = am.findByUser(user);

            if (assignments.isEmpty()) {
                System.out.println("No assignments for user '" + username + "'");
                return;
            }

            System.out.println("\nAssignments for " + user.username() + " (" + user.fullName() + "):");
            System.out.println("-".repeat(60));

            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.println("\n  Role: " + ra.role().name());
                System.out.println("  Type: " + ra.assignmentType());
                System.out.println("  Status: " + status);
                System.out.println("  Assigned by: " + ra.metadata().assignedBy());
                System.out.println("  Assigned at: " + ra.metadata().assignedAt());
                if (ra.metadata().reason() != null && !ra.metadata().reason().isBlank()) {
                    System.out.println("  Reason: " + ra.metadata().reason());
                }
                if (ra instanceof TemporaryAssignment) {
                    System.out.println("  Expires: " + ((TemporaryAssignment) ra).getExpiresAt());
                }
            }
            System.out.println("\n" + "-".repeat(60));
            System.out.println("Total: " + assignments.size() + " assignments");
        });

        parser.registerCommand("assignment-list-role", "List users by role", (s, sys) -> {
            System.out.println("\n=== LIST ROLE ASSIGNMENTS ===");

            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();

            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<Role> roleOpt = rm.findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role '" + roleName + "' not found");
                return;
            }
            Role role = roleOpt.get();

            List<RoleAssignment> assignments = am.findByRole(role);

            if (assignments.isEmpty()) {
                System.out.println("No users have role '" + roleName + "'");
                return;
            }

            System.out.println("\nUsers with role '" + roleName + "':");
            System.out.println("-".repeat(50));

            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.println("  - " + ra.user().username() + " (" + ra.user().fullName() + ") - " + status);
            }

            System.out.println("-".repeat(50));
            System.out.println("Total: " + assignments.size() + " users");
        });

        parser.registerCommand("assignment-active", "Show active assignments", (s, sys) -> {
            System.out.println("\n=== ACTIVE ASSIGNMENTS ===");

            List<RoleAssignment> active = sys.getAssignmentManager().getActiveAssignments();

            if (active.isEmpty()) {
                System.out.println("No active assignments.");
                return;
            }

            System.out.println("\n" + "-".repeat(75));
            System.out.printf("| %-15s | %-15s | %-10s | %-20s |\n",
                    "USERNAME", "ROLE", "TYPE", "ASSIGNED AT");
            System.out.println("-".repeat(75));

            for (RoleAssignment ra : active) {
                String assignedAt = ra.metadata().assignedAt();
                if (assignedAt.length() > 20) {
                    assignedAt = assignedAt.substring(0, 17) + "...";
                }

                System.out.printf("| %-15s | %-15s | %-10s | %-20s |\n",
                        ra.user().username(),
                        ra.role().name().length() > 15 ? ra.role().name().substring(0, 12) + "..." : ra.role().name(),
                        ra.assignmentType(),
                        assignedAt
                );
            }
            System.out.println("-".repeat(75));
            System.out.println("Total: " + active.size() + " active assignments");
        });

        parser.registerCommand("assignment-expired", "Show expired assignments", (s, sys) -> {
            System.out.println("\n=== EXPIRED ASSIGNMENTS ===");

            List<RoleAssignment> expired = sys.getAssignmentManager().findAll().stream()
                    .filter(ra -> ra instanceof TemporaryAssignment && !ra.isActive())
                    .collect(java.util.stream.Collectors.toList());

            if (expired.isEmpty()) {
                System.out.println("No expired assignments.");
                return;
            }

            System.out.println("\nExpired temporary assignments:");
            System.out.println("-".repeat(60));

            for (RoleAssignment ra : expired) {
                TemporaryAssignment temp = (TemporaryAssignment) ra;
                System.out.println("  - " + ra.user().username() + " | " + ra.role().name() + " | Expired: " + temp.getExpiresAt());
            }
            System.out.println("-".repeat(60));
            System.out.println("Total: " + expired.size() + " expired assignments");
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (s, sys) -> {
            System.out.println("\n=== EXTEND ASSIGNMENT ===");

            System.out.println("Search by:");
            System.out.println("  1. Assignment ID");
            System.out.println("  2. Username + Role");
            System.out.print("Choice (1-2): ");

            String choice = s.nextLine().trim();

            AssignmentManager am = sys.getAssignmentManager();
            TemporaryAssignment temp = null;

            if (choice.equals("1")) {
                System.out.print("Assignment ID: ");
                String assignmentId = s.nextLine().trim();

                Optional<RoleAssignment> assignmentOpt = am.findById(assignmentId);
                if (assignmentOpt.isEmpty()) {
                    System.out.println("Assignment not found");
                    return;
                }

                RoleAssignment ra = assignmentOpt.get();
                if (!(ra instanceof TemporaryAssignment)) {
                    System.out.println("Only temporary assignments can be extended");
                    return;
                }
                temp = (TemporaryAssignment) ra;

            } else if (choice.equals("2")) {
                System.out.print("Username: ");
                String username = s.nextLine().trim();
                System.out.print("Role name: ");
                String roleName = s.nextLine().trim();

                UserManager um = sys.getUserManager();
                RoleManager rm = sys.getRoleManager();

                Optional<User> userOpt = um.findByUsername(username);
                Optional<Role> roleOpt = rm.findByName(roleName);

                if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                    System.out.println("User or role not found");
                    return;
                }

                User user = userOpt.get();
                Role role = roleOpt.get();

                List<RoleAssignment> assignments = am.findByUser(user).stream()
                        .filter(ra -> ra.role().equals(role))
                        .filter(ra -> ra instanceof TemporaryAssignment)
                        .collect(java.util.stream.Collectors.toList());

                if (assignments.isEmpty()) {
                    System.out.println("No temporary assignment found for this user and role");
                    return;
                }

                if (assignments.size() > 1) {
                    System.out.println("Multiple assignments found:");
                    for (int i = 0; i < assignments.size(); i++) {
                        System.out.println("  " + (i + 1) + ". ID: " + assignments.get(i).assignmentId());
                    }
                    System.out.print("Select assignment (number): ");
                    int idx = Integer.parseInt(s.nextLine().trim()) - 1;
                    if (idx >= 0 && idx < assignments.size()) {
                        temp = (TemporaryAssignment) assignments.get(idx);
                    } else {
                        System.out.println("Invalid selection");
                        return;
                    }
                } else {
                    temp = (TemporaryAssignment) assignments.get(0);
                }

            } else {
                System.out.println("Invalid choice");
                return;
            }

            if (temp != null) {
                System.out.println("\nCurrent expiration: " + temp.getExpiresAt());
                System.out.print("New expiration date (YYYY-MM-DD): ");
                String newDate = s.nextLine().trim();

                temp.extend(newDate);
                System.out.println("Assignment extended successfully!");
                System.out.println("  User: " + temp.user().username());
                System.out.println("  Role: " + temp.role().name());
                System.out.println("  New expiration: " + newDate);
            }
        });

        parser.registerCommand("assignment-search", "Search assignments", (s, sys) -> {
            System.out.println("\n=== SEARCH ASSIGNMENTS ===");
            System.out.println("Select filter:");
            System.out.println("  1. By user");
            System.out.println("  2. By role");
            System.out.println("  3. By type (permanent/temporary)");
            System.out.println("  4. By status (active/inactive)");
            System.out.println("  5. Assigned after date");
            System.out.println("  6. Expiring before date");
            System.out.print("Choice (1-6): ");

            String choice = s.nextLine().trim();

            AssignmentManager am = sys.getAssignmentManager();
            List<RoleAssignment> allAssignments = am.findAll();
            List<RoleAssignment> results = new java.util.ArrayList<>();
            String filterDesc = "";

            try {
                if (choice.equals("1")) {
                    System.out.print("Username: ");
                    String username = s.nextLine().trim();
                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("User not found");
                        return;
                    }
                    results = am.findByUser(userOpt.get());
                    filterDesc = "user = " + username;

                } else if (choice.equals("2")) {
                    System.out.print("Role name: ");
                    String roleName = s.nextLine().trim();
                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Role not found");
                        return;
                    }
                    results = am.findByRole(roleOpt.get());
                    filterDesc = "role = " + roleName;

                } else if (choice.equals("3")) {
                    System.out.print("Type (permanent/temporary): ");
                    String type = s.nextLine().trim().toLowerCase();
                    results = allAssignments.stream()
                            .filter(ra -> ra.assignmentType().toLowerCase().equals(type))
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "type = " + type;

                } else if (choice.equals("4")) {
                    System.out.print("Status (active/inactive): ");
                    String status = s.nextLine().trim().toLowerCase();
                    boolean active = status.equals("active");
                    results = allAssignments.stream()
                            .filter(ra -> ra.isActive() == active)
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "status = " + status;

                } else if (choice.equals("5")) {
                    System.out.print("Date (YYYY-MM-DD): ");
                    String date = s.nextLine().trim();
                    results = allAssignments.stream()
                            .filter(ra -> ra.metadata().assignedAt().compareTo(date) > 0)
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "assigned after " + date;

                } else if (choice.equals("6")) {
                    System.out.print("Date (YYYY-MM-DD): ");
                    String date = s.nextLine().trim();
                    results = allAssignments.stream()
                            .filter(ra -> ra instanceof TemporaryAssignment)
                            .filter(ra -> ((TemporaryAssignment) ra).getExpiresAt().compareTo(date) < 0)
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "expiring before " + date;

                } else {
                    System.out.println("Invalid choice");
                    return;
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }

            if (results.isEmpty()) {
                System.out.println("\nNo assignments found with filter: " + filterDesc);
            } else {
                System.out.println("\nSearch results for: " + filterDesc);
                System.out.println("\n" + "-".repeat(70));
                System.out.printf("| %-15s | %-15s | %-10s | %-8s |\n",
                        "USERNAME", "ROLE", "TYPE", "STATUS");
                System.out.println("-".repeat(70));

                for (RoleAssignment ra : results) {
                    System.out.printf("| %-15s | %-15s | %-10s | %-8s |\n",
                            ra.user().username(),
                            ra.role().name().length() > 15 ? ra.role().name().substring(0, 12) + "..." : ra.role().name(),
                            ra.assignmentType(),
                            ra.isActive() ? "ACTIVE" : "INACTIVE"
                    );
                }
                System.out.println("-".repeat(70));
                System.out.println("Total: " + results.size() + " assignments");
            }
        });

        //Команды просмотра прав:

        parser.registerCommand("permissions-user", "Show all permissions of a user", (s, sys) -> {
            System.out.println("\n=== USER PERMISSIONS ===");

            System.out.print("Username: ");
            String username = s.nextLine().trim();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            var permissions = am.getUserPermissions(user);

            if (permissions.isEmpty()) {
                System.out.println("\nUser '" + username + "' has no permissions.");
                return;
            }

            System.out.println("\nPermissions for " + user.username() + " (" + user.fullName() + "):");
            System.out.println("-".repeat(50));

            java.util.Map<String, java.util.Set<String>> groupedByResource = new java.util.HashMap<>();

            for (Permission p : permissions) {
                groupedByResource.computeIfAbsent(p.resource(), k -> new java.util.HashSet<>()).add(p.name());
            }

            for (java.util.Map.Entry<String, java.util.Set<String>> entry : groupedByResource.entrySet()) {
                System.out.println("\n  Resource: " + entry.getKey());
                System.out.println("    Actions: " + String.join(", ", entry.getValue()));
            }

            System.out.println("\n" + "-".repeat(50));
            System.out.println("Total permissions: " + permissions.size());
        });

        parser.registerCommand("permissions-check", "Check if user has specific permission", (s, sys) -> {
            System.out.println("\n=== CHECK PERMISSION ===");

            System.out.print("Username: ");
            String username = s.nextLine().trim();

            System.out.print("Permission name (READ, WRITE, DELETE): ");
            String permName = s.nextLine().trim().toUpperCase();

            System.out.print("Resource (users, reports, *): ");
            String resource = s.nextLine().trim().toLowerCase();

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            boolean hasPermission = am.userHasPermission(user, permName, resource);

            System.out.println("\n" + "-".repeat(50));
            System.out.println("User: " + username);
            System.out.println("Permission: " + permName + " on " + resource);
            System.out.println("Result: " + (hasPermission ? "HAS PERMISSION" : "NO PERMISSION"));

            if (hasPermission) {
                System.out.println("\nGranted through roles:");
                List<RoleAssignment> assignments = am.findByUser(user);
                for (RoleAssignment ra : assignments) {
                    if (ra.isActive() && ra.role().hasPermission(permName, resource)) {
                        System.out.println("  - " + ra.role().name() + " (" + ra.assignmentType() + ")");
                    }
                }
            }
            System.out.println("-".repeat(50));
        });

        //Служебные команды:

        parser.registerCommand("help", "Show all commands", (s, sys) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Show system statistics", (s, sys) -> {
            System.out.println(sys.generateStatistics());

            System.out.println("\n=== DETAILED STATISTICS ===");

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            List<RoleAssignment> allAssignments = am.findAll();
            long activeAssignments = allAssignments.stream().filter(RoleAssignment::isActive).count();
            long expiredAssignments = allAssignments.size() - activeAssignments;

            System.out.println("Total assignments: " + allAssignments.size());
            System.out.println("Active assignments: " + activeAssignments);
            System.out.println("Expired assignments: " + expiredAssignments);

            int userCount = um.count();
            if (userCount > 0) {
                double avgRolesPerUser = (double) allAssignments.size() / userCount;
                System.out.printf("Average roles per user: %.2f\n", avgRolesPerUser);
            } else {
                System.out.println("Average roles per user: 0");
            }

            java.util.Map<String, Long> roleCount = new java.util.HashMap<>();
            for (RoleAssignment ra : allAssignments) {
                String roleName = ra.role().name();
                roleCount.put(roleName, roleCount.getOrDefault(roleName, 0L) + 1);
            }

            System.out.println("\nTop 3 most popular roles:");
            roleCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(3)
                    .forEach(e -> System.out.println("  - " + e.getKey() + " (" + e.getValue() + " assignments)"));

            if (roleCount.isEmpty()) {
                System.out.println("  No roles assigned yet");
            }
        });

        parser.registerCommand("clear", "Clear the screen", (s, sys) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit the program", (s, sys) -> {
            System.out.print("\nAre you sure you want to exit? (y/n): ");
            String answer = s.nextLine().trim().toLowerCase();

            if (answer.equals("yes") || answer.equals("y")) {
                System.exit(0);
            } else {
                System.out.println("Exit cancelled.");
            }
        });

        parser.registerCommand("save", "Save data to file", (s, sys) -> {
            System.out.println("\n=== SAVE DATA ===");

            System.out.print("Enter filename: ");
            String filename = s.nextLine().trim();

            if (filename.isEmpty()) {
                filename = "rbac_data.txt";
            }

            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {

                writer.println("[USERS]");
                for (User user : sys.getUserManager().findAll()) {
                    writer.println(user.username() + "|" + user.fullName() + "|" + user.email());
                }

                writer.println("\n[ROLES]");
                for (Role role : sys.getRoleManager().findAll()) {
                    writer.print(role.name() + "|" + role.getDescription());
                    for (Permission p : role.getPermissions()) {
                        writer.print("|" + p.name() + ":" + p.resource() + ":" + p.description());
                    }
                    writer.println();
                }

                writer.println("\n[ASSIGNMENTS]");
                for (RoleAssignment ra : sys.getAssignmentManager().findAll()) {
                    String type = ra.assignmentType();
                    String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                    writer.print(ra.user().username() + "|" + ra.role().name() + "|" + type + "|" + status);
                    writer.print("|" + ra.metadata().assignedBy() + "|" + ra.metadata().assignedAt() + "|" + ra.metadata().reason());

                    if (ra instanceof TemporaryAssignment) {
                        writer.print("|" + ((TemporaryAssignment) ra).getExpiresAt());
                    }
                    writer.println();
                }

                System.out.println("Data saved to '" + filename + "' successfully!");

            } catch (java.io.IOException e) {
                System.out.println("Error saving data: " + e.getMessage());
            }
        });

        parser.registerCommand("load", "Load data from file", (s, sys) -> {
            System.out.println("\n=== LOAD DATA ===");

            System.out.print("Enter filename: ");
            String filename = s.nextLine().trim();

            if (filename.isEmpty()) {
                System.out.println("No filename provided");
                return;
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(filename))) {

                String line;
                String section = "";

                UserManager um = sys.getUserManager();
                RoleManager rm = sys.getRoleManager();
                AssignmentManager am = sys.getAssignmentManager();

                um.clear();
                rm.clear();
                am.clear();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty()) continue;

                    if (line.equals("[USERS]")) {
                        section = "USERS";
                        continue;
                    } else if (line.equals("[ROLES]")) {
                        section = "ROLES";
                        continue;
                    } else if (line.equals("[ASSIGNMENTS]")) {
                        section = "ASSIGNMENTS";
                        continue;
                    }

                    if (section.equals("USERS")) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 3) {
                            try {
                                User user = User.create(parts[0], parts[1], parts[2]);
                                um.add(user);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Skipping user: " + e.getMessage());
                            }
                        }

                    } else if (section.equals("ROLES")) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 2) {
                            Role role = new Role(parts[0], parts[1]);
                            for (int i = 2; i < parts.length; i++) {
                                String[] permParts = parts[i].split(":");
                                if (permParts.length >= 3) {
                                    try {
                                        Permission p = new Permission(permParts[0], permParts[1], permParts[2]);
                                        role.addPermission(p);
                                    } catch (IllegalArgumentException e) {
                                        System.out.println("Skipping permission: " + e.getMessage());
                                    }
                                }
                            }
                            rm.add(role);
                        }

                    } else if (section.equals("ASSIGNMENTS")) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 7) {
                            String username = parts[0];
                            String roleName = parts[1];
                            String type = parts[2];
                            String assignedBy = parts[4];
                            String assignedAt = parts[5];
                            String reason = parts[6];
                            String expiresAt = parts.length > 7 ? parts[7] : null;

                            Optional<User> userOpt = um.findByUsername(username);
                            Optional<Role> roleOpt = rm.findByName(roleName);

                            if (userOpt.isPresent() && roleOpt.isPresent()) {
                                AssignmentMetadata metadata = new AssignmentMetadata(assignedBy, assignedAt, reason);

                                try {
                                    if (type.equals("PERMANENT")) {
                                        PermanentAssignment assignment = new PermanentAssignment(userOpt.get(), roleOpt.get(), metadata);
                                        am.add(assignment);
                                    } else if (type.equals("TEMPORARY") && expiresAt != null) {
                                        TemporaryAssignment assignment = new TemporaryAssignment(userOpt.get(), roleOpt.get(), metadata, expiresAt, false);
                                        am.add(assignment);
                                    }
                                } catch (IllegalStateException e) {
                                    System.out.println("Skipping assignment: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                System.out.println("Data loaded from '" + filename + "' successfully!");
                System.out.println("Users: " + um.count());
                System.out.println("Roles: " + rm.count());
                System.out.println("Assignments: " + am.count());

            } catch (java.io.FileNotFoundException e) {
                System.out.println("File not found: " + filename);
            } catch (java.io.IOException e) {
                System.out.println("Error loading data: " + e.getMessage());
            }
        });

    }
}
