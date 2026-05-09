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
            ConsoleUtils.printHeader("USER LIST");

            List<User> users = sys.getUserManager().findAll();

            boolean useFilter = ConsoleUtils.promptYesNo(s, "Apply filter?");

            if (useFilter) {
                List<String> filterOptions = List.of("By username", "By email");
                String filterType = ConsoleUtils.promptChoice(s, "Select filter type:", filterOptions);
                String filterValue = ConsoleUtils.promptString(s, "Enter search term: ", true);

                if (filterType.equals("By username")) {
                    users = users.stream()
                            .filter(u -> u.username().toLowerCase().contains(filterValue.toLowerCase()))
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    users = users.stream()
                            .filter(u -> u.email().toLowerCase().contains(filterValue.toLowerCase()))
                            .collect(java.util.stream.Collectors.toList());
                }
            }

            if (users.isEmpty()) {
                ConsoleUtils.printInfo("No users found.");
                return;
            }

            String[] headers = {"USERNAME", "FULL NAME", "EMAIL"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (User user : users) {
                rows.add(new String[]{
                        user.username(),
                        FormatUtils.truncate(user.fullName(), 25),
                        FormatUtils.truncate(user.email(), 20)
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + users.size() + " users");
        });


        parser.registerCommand("user-create", "Create new user", (s, sys) -> {
            ConsoleUtils.printHeader("CREATE NEW USER");

            try {
                String username = ConsoleUtils.promptString(s, "Username: ", true);

                if (!ValidationUtils.isValidUsername(username)) {
                    ConsoleUtils.printError("Username must be 3-20 characters and contain only letters, numbers, underscores");
                    return;
                }

                String fullName = ConsoleUtils.promptString(s, "Full Name: ", true);
                String email = ConsoleUtils.promptString(s, "Email: ", true);

                if (!ValidationUtils.isValidEmail(email)) {
                    ConsoleUtils.printError("Invalid email format");
                    return;
                }

                User user = User.create(username, fullName, email);
                sys.getUserManager().add(user);

                sys.getAuditLog().log("CREATE_USER", sys.getCurrentUser(), username,
                        "Full name: " + fullName + ", Email: " + email);

                ConsoleUtils.printSuccess("User created successfully!");
                System.out.println("  Username: " + user.username());
                System.out.println("  Full Name: " + user.fullName());
                System.out.println("  Email: " + user.email());

            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtils.printError("Error creating user: " + e.getMessage());
            }
        });


        parser.registerCommand("user-view", "View user", (s, sys) -> {
            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                System.out.println(FormatUtils.formatHeader("USER INFORMATION"));
                System.out.println(FormatUtils.formatBox(user.username()));

                System.out.println(FormatUtils.formatHeader("DETAILS"));
                System.out.println("  Full Name: " + user.fullName());
                System.out.println("  Email: " + user.email());

                List<RoleAssignment> assignments = am.findByUser(user);
                System.out.println(FormatUtils.formatHeader("ASSIGNED ROLES (" + assignments.size() + ")"));

                if (assignments.isEmpty()) {
                    System.out.println("  No roles assigned");
                } else {
                    String[] headers = {"ROLE", "TYPE", "STATUS", "ASSIGNED BY", "ASSIGNED AT"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (RoleAssignment ra : assignments) {
                        rows.add(new String[]{
                                ra.role().name(),
                                ra.assignmentType(),
                                ra.isActive() ? "ACTIVE" : "INACTIVE",
                                ra.metadata().assignedBy(),
                                FormatUtils.truncate(ra.metadata().assignedAt(), 16)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                }

                var permissions = am.getUserPermissions(user);
                System.out.println(FormatUtils.formatHeader("ALL PERMISSIONS (" + permissions.size() + ")"));

                if (permissions.isEmpty()) {
                    System.out.println("  No permissions");
                } else {
                    String[] headers = {"PERMISSION", "RESOURCE", "DESCRIPTION"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (Permission p : permissions) {
                        rows.add(new String[]{
                                p.name(),
                                p.resource(),
                                FormatUtils.truncate(p.description(), 30)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                }

            } else {
                ConsoleUtils.printError("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-update", "Update user information", (s, sys) -> {
            ConsoleUtils.printHeader("USER UPDATE");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()){
                User user = optionalUser.get();

                ConsoleUtils.printInfo("Current information:");
                System.out.println("  Full Name: " + user.fullName());
                System.out.println("  Email: " + user.email());

                String newFullName = ConsoleUtils.promptString(s, "Enter new Full name (Enter to keep current): ", false);
                if (newFullName == null || newFullName.isEmpty()) {
                    newFullName = user.fullName();
                }

                String newEmail = ConsoleUtils.promptString(s, "Enter new Email (Enter to keep current): ", false);
                if (newEmail == null || newEmail.isEmpty()) {
                    newEmail = user.email();
                } else {
                    if (!ValidationUtils.isValidEmail(newEmail)) {
                        ConsoleUtils.printError("Invalid email format");
                        return;
                    }
                }

                try {
                    um.update(username, newFullName, newEmail);
                    ConsoleUtils.printSuccess("User updated successfully!");

                    User updated = um.findByUsername(username).get();
                    System.out.println("  Username: " + updated.username());
                    System.out.println("  Full Name: " + updated.fullName());
                    System.out.println("  Email: " + updated.email());
                } catch (IllegalArgumentException e) {
                    ConsoleUtils.printError(e.getMessage());
                }
            } else {
                ConsoleUtils.printError("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-delete", "Delete User", (s, sys) -> {
            ConsoleUtils.printHeader("DELETE USER");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> optionalUser = um.findByUsername(username);

            if (optionalUser.isPresent()){
                User user = optionalUser.get();

                List<RoleAssignment> assignments = am.findByUser(user);
                int rolesCount = assignments.size();

                if (rolesCount > 0) {
                    ConsoleUtils.printInfo("User has " + rolesCount + " role assignment(s)");
                }

                boolean confirmed = ConsoleUtils.promptYesNo(s, "Are you sure you want to delete this user?");

                if (confirmed){
                    for (RoleAssignment ra : assignments) {
                        am.remove(ra);
                    }

                    um.remove(user);

                    sys.getAuditLog().log("DELETE_USER", sys.getCurrentUser(), username,
                            "Removed " + rolesCount + " role assignment(s)");

                    ConsoleUtils.printSuccess("User '" + username + "' deleted successfully!");
                } else {
                    ConsoleUtils.printInfo("Deletion cancelled");
                }
            } else{
                ConsoleUtils.printError("User '" + username + "' not found");
            }
        });

        parser.registerCommand("user-search", "Search user with filters", (s, sys) -> {
            ConsoleUtils.printHeader("Search Users");

            List<String> filterOptions = List.of(
                    "By username (contains)",
                    "By email (contains)",
                    "By email domain",
                    "By full name (contains)"
            );

            String choice = ConsoleUtils.promptChoice(s, "Select filter:", filterOptions);

            List<User> allUsers = sys.getUserManager().findAll();
            List<User> results = new java.util.ArrayList<>();
            String filterDesc = "";

            if (choice.equals("By username (contains)")) {
                String part = ConsoleUtils.promptString(s, "Enter username part: ", true);
                results = allUsers.stream()
                        .filter(u -> u.username().toLowerCase().contains(part.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "username contains '" + part + "'";

            } else if (choice.equals("By email (contains)")) {
                String part = ConsoleUtils.promptString(s, "Enter email part: ", true);
                results = allUsers.stream()
                        .filter(u -> u.email().toLowerCase().contains(part.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "email contains '" + part + "'";

            } else if (choice.equals("By email domain")) {
                String domain = ConsoleUtils.promptString(s, "Enter email domain: ", true);
                results = allUsers.stream()
                        .filter(u -> u.email().toLowerCase().endsWith(domain.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "email domain '" + domain + "'";

            } else {
                String part = ConsoleUtils.promptString(s, "Enter full name part: ", true);
                results = allUsers.stream()
                        .filter(u -> u.fullName().toLowerCase().contains(part.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "full name contains '" + part + "'";
            }

            if (results.isEmpty()) {
                ConsoleUtils.printInfo("No users found with filter: " + filterDesc);
            } else {
                ConsoleUtils.printSuccess("Search results for: " + filterDesc);

                String[] headers = {"USERNAME", "FULL NAME", "EMAIL"};
                List<String[]> rows = new java.util.ArrayList<>();

                for (User user : results) {
                    rows.add(new String[]{
                            user.username(),
                            FormatUtils.truncate(user.fullName(), 25),
                            FormatUtils.truncate(user.email(), 30)
                    });
                }

                System.out.println(FormatUtils.formatTable(headers, rows));
                ConsoleUtils.printSuccess("Total: " + results.size() + " users");
            }
        });

        //Команды управления ролями:

        parser.registerCommand("role-list", "List all roles", (s, sys) -> {
            ConsoleUtils.printHeader("ROLE LIST");

            List<Role> roles = sys.getRoleManager().findAll();

            if (roles.isEmpty()) {
                ConsoleUtils.printInfo("No roles found.");
                return;
            }

            String[] headers = {"ROLE NAME", "PERMISSIONS", "ROLE ID"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (Role role : roles) {
                rows.add(new String[]{
                        role.name(),
                        String.valueOf(role.getPermissions().size()),
                        FormatUtils.truncate(role.getId(), 30)
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + roles.size() + " roles");
        });

        parser.registerCommand("role-create", "Create new role", (s, sys) -> {
            ConsoleUtils.printHeader("CREATE NEW ROLE");

            try{
                String name = ConsoleUtils.promptString(s, "Role name: ", true);

                if (name == null || name.trim().isEmpty()) {
                    ConsoleUtils.printError("Role name cannot be empty");
                    return;
                }

                String description = ConsoleUtils.promptString(s, "Role description: ", false);
                if (description == null) description = "";

                Role role = new Role(name, description);
                sys.getRoleManager().add(role);

                sys.getAuditLog().log("CREATE_ROLE", sys.getCurrentUser(), name,
                        "Description: " + description);

                ConsoleUtils.printSuccess("Role '" + name + "' created successfully!");
                System.out.println("  ID: " + role.getId());
                System.out.println("  Description: " + role.getDescription());

                boolean addPermissions = ConsoleUtils.promptYesNo(s, "Do you want to add permissions to this role?");

                if (addPermissions) {
                    boolean adding = true;

                    while (adding) {
                        ConsoleUtils.printHeader("Add Permission");

                        String permName = ConsoleUtils.promptString(s, "Permission name (READ, WRITE, DELETE): ", true);
                        if (permName != null) permName = permName.toUpperCase();

                        String resource = ConsoleUtils.promptString(s, "Resource (users, reports, *): ", true);
                        if (resource != null) resource = resource.toLowerCase();

                        String permDesc = ConsoleUtils.promptString(s, "Description: ", false);
                        if (permDesc == null) permDesc = "";

                        try {
                            Permission permission = new Permission(permName, resource, permDesc);
                            role.addPermission(permission);
                            ConsoleUtils.printSuccess("Permission added successfully!");

                            adding = ConsoleUtils.promptYesNo(s, "Add another permission?");

                        } catch (IllegalArgumentException e) {
                            ConsoleUtils.printError(e.getMessage());
                        }
                    }

                    ConsoleUtils.printSuccess("Role '" + name + "' now has " + role.getPermissions().size() + " permissions.");
                }

            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtils.printError("Error creating role: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role", (s, sys) -> {
            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                System.out.println(FormatUtils.formatHeader("ROLE INFORMATION"));
                System.out.println(FormatUtils.formatBox(role.name()));

                System.out.println("  Description: " + role.getDescription());
                System.out.println("  ID: " + role.getId());

                System.out.println(FormatUtils.formatHeader("PERMISSIONS (" + role.getPermissions().size() + ")"));

                if (role.getPermissions().isEmpty()) {
                    System.out.println("  No permissions");
                } else {
                    String[] headers = {"PERMISSION", "RESOURCE", "DESCRIPTION"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (Permission p : role.getPermissions()) {
                        rows.add(new String[]{
                                p.name(),
                                p.resource(),
                                FormatUtils.truncate(p.description(), 40)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                }

            } else {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-update", "Update role (name/description)", (s, sys) -> {
            ConsoleUtils.printHeader("UPDATE ROLE");

            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                ConsoleUtils.printInfo("Current information:");
                System.out.println("  Name: " + role.name());
                System.out.println("  Description: " + role.getDescription());

                System.out.println("\nEnter new data (press Enter to keep current):");

                String newName = ConsoleUtils.promptString(s, "New role name: ", false);
                if (newName == null || newName.isEmpty()) {
                    newName = role.name();
                }

                String newDescription = ConsoleUtils.promptString(s, "New description: ", false);
                if (newDescription == null || newDescription.isEmpty()) {
                    newDescription = role.getDescription();
                }

                try {
                    if (!newName.equals(role.name())) {
                        if (rm.findByName(newName).isPresent()) {
                            ConsoleUtils.printError("Role with name '" + newName + "' already exists");
                            return;
                        }
                    }

                    Role updatedRole = new Role(newName, newDescription);
                    for (Permission p : role.getPermissions()) {
                        updatedRole.addPermission(p);
                    }

                    rm.remove(role);
                    rm.add(updatedRole);

                    ConsoleUtils.printSuccess("Role updated successfully!");
                    System.out.println("  Name: " + updatedRole.name());
                    System.out.println("  Description: " + updatedRole.getDescription());
                    System.out.println("  Permissions: " + updatedRole.getPermissions().size());

                } catch (IllegalArgumentException e) {
                    ConsoleUtils.printError(e.getMessage());
                } catch (Exception e) {
                    ConsoleUtils.printError("Error updating role: " + e.getMessage());
                }

            } else {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-delete", "Delete role", (s, sys) -> {
            ConsoleUtils.printHeader("DELETE ROLE");

            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                List<RoleAssignment> assignments = am.findByRole(role);
                int usersCount = assignments.size();

                if (!assignments.isEmpty()) {
                    ConsoleUtils.printInfo("Role is assigned to " + usersCount + " user(s):");
                    for (RoleAssignment ra : assignments) {
                        System.out.println("  - " + ra.user().username() + " (" + ra.user().fullName() + ")");
                    }
                    ConsoleUtils.printInfo("Deleting this role will remove these assignments!");
                }

                boolean confirmed = ConsoleUtils.promptYesNo(s, "Are you sure you want to delete this role?");

                if (confirmed) {
                    for (RoleAssignment ra : assignments) {
                        am.remove(ra);
                    }
                    rm.remove(role);

                    sys.getAuditLog().log("DELETE_ROLE", sys.getCurrentUser(), roleName,
                            "Removed from " + usersCount + " user(s)");

                    ConsoleUtils.printSuccess("Role '" + roleName + "' deleted successfully!");
                } else {
                    ConsoleUtils.printInfo("Deletion cancelled");
                }

            } else {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (s, sys) -> {
            ConsoleUtils.printHeader("ADD PERMISSION TO ROLE");

            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                ConsoleUtils.printInfo("Current permissions (" + role.getPermissions().size() + "):");
                for (Permission p : role.getPermissions()) {
                    System.out.println("  - " + p.name() + " on " + p.resource());
                }

                ConsoleUtils.printHeader("New Permission");

                String permName = ConsoleUtils.promptString(s, "Permission name (READ, WRITE, DELETE): ", true);
                if (permName != null) permName = permName.toUpperCase();

                String resource = ConsoleUtils.promptString(s, "Resource (users, reports, *): ", true);
                if (resource != null) resource = resource.toLowerCase();

                String description = ConsoleUtils.promptString(s, "Description: ", false);
                if (description == null) description = "";

                try {
                    Permission permission = new Permission(permName, resource, description);
                    role.addPermission(permission);
                    ConsoleUtils.printSuccess("Permission added successfully!");
                    System.out.println("  " + permission.format());
                } catch (IllegalArgumentException e) {
                    ConsoleUtils.printError(e.getMessage());
                }

            } else {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (s, sys) -> {
            ConsoleUtils.printHeader("REMOVE PERMISSION FROM ROLE");

            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            Optional<Role> roleOpt = rm.findByName(roleName);

            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();

                List<Permission> permissions = role.getPermissions().stream().collect(java.util.stream.Collectors.toList());

                if (permissions.isEmpty()) {
                    ConsoleUtils.printInfo("Role has no permissions.");
                    return;
                }

                System.out.println("\nPermissions:");
                for (int i = 0; i < permissions.size(); i++) {
                    Permission p = permissions.get(i);
                    System.out.println("  " + (i + 1) + ". " + p.name() + " on " + p.resource());
                }

                int choice = ConsoleUtils.promptInt(s, "Enter number to remove", 1, permissions.size());
                Permission toRemove = permissions.get(choice - 1);

                role.removePermission(toRemove);
                ConsoleUtils.printSuccess("Permission removed: " + toRemove.name() + " on " + toRemove.resource());

            } else {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
            }
        });

        parser.registerCommand("role-search", "Search roles", (s, sys) -> {
            ConsoleUtils.printHeader("SEARCH ROLES");

            List<String> filterOptions = List.of(
                    "By name (contains)",
                    "By permission (has specific permission)",
                    "By minimum number of permissions"
            );

            String choice = ConsoleUtils.promptChoice(s, "Select filter:", filterOptions);

            List<Role> allRoles = sys.getRoleManager().findAll();
            List<Role> results = new java.util.ArrayList<>();
            String filterDesc = "";

            if (choice.equals("By name (contains)")) {
                String part = ConsoleUtils.promptString(s, "Enter role name part: ", true);
                results = allRoles.stream()
                        .filter(r -> r.name().toLowerCase().contains(part.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "name contains '" + part + "'";

            } else if (choice.equals("By permission (has specific permission)")) {
                String permName = ConsoleUtils.promptString(s, "Enter permission name (READ, WRITE, DELETE): ", true);
                String resource = ConsoleUtils.promptString(s, "Enter resource (users, reports, *): ", true);

                results = allRoles.stream()
                        .filter(r -> r.hasPermission(permName.toUpperCase(), resource.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "has permission " + permName + " on " + resource;

            } else {
                int min = ConsoleUtils.promptInt(s, "Enter minimum number of permissions", 0, 100);
                results = allRoles.stream()
                        .filter(r -> r.getPermissions().size() >= min)
                        .collect(java.util.stream.Collectors.toList());
                filterDesc = "has at least " + min + " permissions";
            }

            if (results.isEmpty()) {
                ConsoleUtils.printInfo("No roles found with filter: " + filterDesc);
            } else {
                ConsoleUtils.printSuccess("Search results for: " + filterDesc);

                String[] headers = {"ROLE NAME", "PERMISSIONS", "ROLE ID"};
                List<String[]> rows = new java.util.ArrayList<>();

                for (Role role : results) {
                    rows.add(new String[]{
                            role.name(),
                            String.valueOf(role.getPermissions().size()),
                            FormatUtils.truncate(role.getId(), 30)
                    });
                }

                System.out.println(FormatUtils.formatTable(headers, rows));
                ConsoleUtils.printSuccess("Total: " + results.size() + " roles");
            }
        });

        //Команды управления назначениями:

        parser.registerCommand("assign-role", "Assign role to user", (s, sys) -> {
            ConsoleUtils.printHeader("ASSIGN ROLE");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<Role> roles = rm.findAll();
            if (roles.isEmpty()) {
                ConsoleUtils.printError("No roles available. Create a role first.");
                return;
            }

            Role role = ConsoleUtils.promptChoice(s, "Select role:", roles);

            String type = ConsoleUtils.promptString(s, "Assignment type (permanent/temporary): ", true);
            String reason = ConsoleUtils.promptString(s, "Reason: ", false);
            if (reason == null) reason = "";

            AssignmentMetadata metadata = AssignmentMetadata.now(sys.getCurrentUser(), reason);

            try {
                if (type.equalsIgnoreCase("permanent") || type.equals("p")) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    am.add(assignment);

                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(),
                            username + " -> " + role.name(), "Type: PERMANENT, Reason: " + reason);

                    ConsoleUtils.printSuccess("Permanent role assigned successfully!");
                    System.out.println("  User: " + username);
                    System.out.println("  Role: " + role.name());

                } else if (type.equalsIgnoreCase("temporary") || type.equals("t")) {
                    String expiresAt = ConsoleUtils.promptString(s, "Expiration date (YYYY-MM-DD): ", true);

                    if (!DateUtils.isValidDateFormat(expiresAt)) {
                        ConsoleUtils.printError("Invalid date format. Use YYYY-MM-DD");
                        return;
                    }

                    if (DateUtils.isBefore(expiresAt, DateUtils.getCurrentDate())) {
                        ConsoleUtils.printError("Expiration date must be in the future");
                        return;
                    }

                    boolean autoRenew = ConsoleUtils.promptYesNo(s, "Auto-renew?");

                    ConsoleUtils.printInfo("Expires " + DateUtils.formatRelativeTime(expiresAt));

                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    am.add(assignment);

                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(),
                            username + " -> " + role.name(), "Type: TEMPORARY, Expires: " + expiresAt + ", Reason: " + reason);

                    ConsoleUtils.printSuccess("Temporary role assigned successfully!");
                    System.out.println("  User: " + username);
                    System.out.println("  Role: " + role.name());
                    System.out.println("  Expires: " + expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")");
                } else {
                    ConsoleUtils.printError("Invalid type. Use 'permanent' or 'temporary'");
                }

            } catch (IllegalStateException e) {
                ConsoleUtils.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtils.printError("Error creating assignment: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (s, sys) -> {
            ConsoleUtils.printHeader("REVOKE ROLE");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> activeAssignments = am.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(java.util.stream.Collectors.toList());

            if (activeAssignments.isEmpty()) {
                ConsoleUtils.printError("No active assignments for user '" + username + "'");
                return;
            }

            RoleAssignment toRevoke = ConsoleUtils.promptChoice(s, "Select assignment to revoke:", activeAssignments);
            String roleName = toRevoke.role().name();

            boolean confirmed = ConsoleUtils.promptYesNo(s, "Are you sure you want to revoke this role?");

            if (!confirmed) {
                ConsoleUtils.printInfo("Operation cancelled");
                return;
            }

            if (toRevoke instanceof PermanentAssignment) {
                ((PermanentAssignment) toRevoke).revoke();

                sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(),
                        username + " <- " + roleName, "Type: PERMANENT");

                ConsoleUtils.printSuccess("Permanent assignment revoked successfully!");
                System.out.println("  User: " + username);
                System.out.println("  Role: " + roleName);

            } else if (toRevoke instanceof TemporaryAssignment) {
                String yesterday = java.time.LocalDate.now().minusDays(1).toString();
                ((TemporaryAssignment) toRevoke).extend(yesterday);

                sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(),
                        username + " <- " + roleName, "Type: TEMPORARY");

                ConsoleUtils.printSuccess("Temporary assignment expired successfully!");
                System.out.println("  User: " + username);
                System.out.println("  Role: " + roleName);
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (s, sys) -> {
            ConsoleUtils.printHeader("ASSIGNMENT LIST");

            List<RoleAssignment> assignments = sys.getAssignmentManager().findAll();

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("No assignments found.");
                return;
            }

            String[] headers = {"USERNAME", "ROLE", "TYPE", "STATUS", "EXPIRES"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (RoleAssignment ra : assignments) {
                String expiresInfo = "";
                if (ra instanceof TemporaryAssignment) {
                    String expiresAt = ((TemporaryAssignment) ra).getExpiresAt();
                    expiresInfo = expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
                } else {
                    expiresInfo = "PERMANENT";
                }

                rows.add(new String[]{
                        ra.user().username(),
                        FormatUtils.truncate(ra.role().name(), 15),
                        ra.assignmentType(),
                        ra.isActive() ? "ACTIVE" : "INACTIVE",
                        FormatUtils.truncate(expiresInfo, 25)
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + assignments.size() + " assignments");
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (s, sys) -> {
            ConsoleUtils.printHeader("LIST USER ASSIGNMENTS");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> assignments = am.findByUser(user);

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("No assignments for user '" + username + "'");
                return;
            }

            ConsoleUtils.printSuccess("Assignments for " + user.username() + " (" + user.fullName() + ")");

            String[] headers = {"ROLE", "TYPE", "STATUS", "EXPIRES", "ASSIGNED BY"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (RoleAssignment ra : assignments) {
                String expiresInfo = "";
                if (ra instanceof TemporaryAssignment) {
                    String expiresAt = ((TemporaryAssignment) ra).getExpiresAt();
                    expiresInfo = expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
                } else {
                    expiresInfo = "PERMANENT";
                }

                rows.add(new String[]{
                        ra.role().name(),
                        ra.assignmentType(),
                        ra.isActive() ? "ACTIVE" : "INACTIVE",
                        FormatUtils.truncate(expiresInfo, 25),
                        ra.metadata().assignedBy()
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + assignments.size() + " assignments");
        });

        parser.registerCommand("assignment-list-role", "List users by role", (s, sys) -> {
            ConsoleUtils.printHeader("LIST ROLE ASSIGNMENTS");

            String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

            RoleManager rm = sys.getRoleManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<Role> roleOpt = rm.findByName(roleName);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role '" + roleName + "' not found");
                return;
            }
            Role role = roleOpt.get();

            List<RoleAssignment> assignments = am.findByRole(role);

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("No users have role '" + roleName + "'");
                return;
            }

            ConsoleUtils.printSuccess("Users with role '" + roleName + "'");

            String[] headers = {"USERNAME", "FULL NAME", "STATUS", "EXPIRES"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (RoleAssignment ra : assignments) {
                String expiresInfo = "";
                if (ra instanceof TemporaryAssignment) {
                    String expiresAt = ((TemporaryAssignment) ra).getExpiresAt();
                    expiresInfo = DateUtils.formatRelativeTime(expiresAt);
                } else {
                    expiresInfo = "PERMANENT";
                }

                rows.add(new String[]{
                        ra.user().username(),
                        FormatUtils.truncate(ra.user().fullName(), 25),
                        ra.isActive() ? "ACTIVE" : "INACTIVE",
                        expiresInfo
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + assignments.size() + " users");
        });

        parser.registerCommand("assignment-active", "Show active assignments", (s, sys) -> {
            ConsoleUtils.printHeader("ACTIVE ASSIGNMENTS");

            List<RoleAssignment> active = sys.getAssignmentManager().getActiveAssignments();

            if (active.isEmpty()) {
                ConsoleUtils.printInfo("No active assignments.");
                return;
            }

            String[] headers = {"USERNAME", "ROLE", "TYPE", "EXPIRES"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (RoleAssignment ra : active) {
                String expiresInfo = "";
                if (ra instanceof TemporaryAssignment) {
                    String expiresAt = ((TemporaryAssignment) ra).getExpiresAt();
                    expiresInfo = expiresAt + " (" + DateUtils.formatRelativeTime(expiresAt) + ")";
                } else {
                    expiresInfo = "PERMANENT";
                }

                rows.add(new String[]{
                        ra.user().username(),
                        FormatUtils.truncate(ra.role().name(), 15),
                        ra.assignmentType(),
                        FormatUtils.truncate(expiresInfo, 25)
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + active.size() + " active assignments");
        });

        parser.registerCommand("assignment-expired", "Show expired assignments", (s, sys) -> {
            ConsoleUtils.printHeader("EXPIRED ASSIGNMENTS");

            List<RoleAssignment> expired = sys.getAssignmentManager().findAll().stream()
                    .filter(ra -> ra instanceof TemporaryAssignment && !ra.isActive())
                    .collect(java.util.stream.Collectors.toList());

            if (expired.isEmpty()) {
                ConsoleUtils.printInfo("No expired assignments.");
                return;
            }

            String[] headers = {"USERNAME", "ROLE", "EXPIRED", "EXPIRED AGO"};
            List<String[]> rows = new java.util.ArrayList<>();

            for (RoleAssignment ra : expired) {
                TemporaryAssignment temp = (TemporaryAssignment) ra;
                rows.add(new String[]{
                        ra.user().username(),
                        ra.role().name(),
                        temp.getExpiresAt(),
                        DateUtils.formatRelativeTime(temp.getExpiresAt())
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total: " + expired.size() + " expired assignments");
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (s, sys) -> {
            ConsoleUtils.printHeader("EXTEND ASSIGNMENT");

            List<String> searchOptions = List.of("Search by Assignment ID", "Search by Username + Role");
            String searchType = ConsoleUtils.promptChoice(s, "Search by:", searchOptions);

            AssignmentManager am = sys.getAssignmentManager();
            TemporaryAssignment temp = null;

            if (searchType.equals("Search by Assignment ID")) {
                String assignmentId = ConsoleUtils.promptString(s, "Assignment ID: ", true);
                Optional<RoleAssignment> assignmentOpt = am.findById(assignmentId);
                if (assignmentOpt.isEmpty()) {
                    ConsoleUtils.printError("Assignment not found");
                    return;
                }
                RoleAssignment ra = assignmentOpt.get();
                if (!(ra instanceof TemporaryAssignment)) {
                    ConsoleUtils.printError("Only temporary assignments can be extended");
                    return;
                }
                temp = (TemporaryAssignment) ra;

            } else {
                String username = ConsoleUtils.promptString(s, "Username: ", true);
                String roleName = ConsoleUtils.promptString(s, "Role name: ", true);

                UserManager um = sys.getUserManager();
                RoleManager rm = sys.getRoleManager();

                Optional<User> userOpt = um.findByUsername(username);
                Optional<Role> roleOpt = rm.findByName(roleName);

                if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                    ConsoleUtils.printError("User or role not found");
                    return;
                }

                List<RoleAssignment> assignments = am.findByUser(userOpt.get()).stream()
                        .filter(ra -> ra.role().equals(roleOpt.get()))
                        .filter(ra -> ra instanceof TemporaryAssignment)
                        .collect(java.util.stream.Collectors.toList());

                if (assignments.isEmpty()) {
                    ConsoleUtils.printError("No temporary assignment found");
                    return;
                }

                if (assignments.size() > 1) {
                    List<String> assignmentOptions = assignments.stream()
                            .map(ra -> ra.assignmentId() + " - " + ra.role().name())
                            .collect(java.util.stream.Collectors.toList());
                    String selected = ConsoleUtils.promptChoice(s, "Multiple assignments found:", assignmentOptions);
                    temp = (TemporaryAssignment) assignments.get(assignmentOptions.indexOf(selected));
                } else {
                    temp = (TemporaryAssignment) assignments.get(0);
                }
            }

            if (temp != null) {
                ConsoleUtils.printInfo("Current expiration: " + temp.getExpiresAt() +
                        " (" + DateUtils.formatRelativeTime(temp.getExpiresAt()) + ")");

                String newDate = ConsoleUtils.promptString(s, "New expiration date (YYYY-MM-DD): ", true);

                if (!DateUtils.isValidDateFormat(newDate)) {
                    ConsoleUtils.printError("Invalid date format. Use YYYY-MM-DD");
                    return;
                }

                if (DateUtils.isBefore(newDate, DateUtils.getCurrentDate())) {
                    ConsoleUtils.printError("New expiration date must be in the future");
                    return;
                }

                temp.extend(newDate);
                ConsoleUtils.printSuccess("Assignment extended successfully!");
                System.out.println("  User: " + temp.user().username());
                System.out.println("  Role: " + temp.role().name());
                System.out.println("  New expiration: " + newDate + " (" + DateUtils.formatRelativeTime(newDate) + ")");
            }
        });

        parser.registerCommand("assignment-search", "Search assignments", (s, sys) -> {
            ConsoleUtils.printHeader("SEARCH ASSIGNMENTS");

            List<String> filterOptions = List.of(
                    "By user",
                    "By role",
                    "By type (permanent/temporary)",
                    "By status (active/inactive)",
                    "Assigned after date",
                    "Expiring before date"
            );

            String choice = ConsoleUtils.promptChoice(s, "Select filter:", filterOptions);

            AssignmentManager am = sys.getAssignmentManager();
            List<RoleAssignment> allAssignments = am.findAll();
            List<RoleAssignment> results = new java.util.ArrayList<>();
            String filterDesc = "";

            try {
                if (choice.equals("By user")) {
                    String username = ConsoleUtils.promptString(s, "Username: ", true);
                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        ConsoleUtils.printError("User not found");
                        return;
                    }
                    results = am.findByUser(userOpt.get());
                    filterDesc = "user = " + username;

                } else if (choice.equals("By role")) {
                    String roleName = ConsoleUtils.promptString(s, "Role name: ", true);
                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        ConsoleUtils.printError("Role not found");
                        return;
                    }
                    results = am.findByRole(roleOpt.get());
                    filterDesc = "role = " + roleName;

                } else if (choice.equals("By type (permanent/temporary)")) {
                    String type = ConsoleUtils.promptString(s, "Type (permanent/temporary): ", true);
                    results = allAssignments.stream()
                            .filter(ra -> ra.assignmentType().toLowerCase().equals(type.toLowerCase()))
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "type = " + type;

                } else if (choice.equals("By status (active/inactive)")) {
                    String status = ConsoleUtils.promptString(s, "Status (active/inactive): ", true);
                    boolean active = status.equals("active");
                    results = allAssignments.stream()
                            .filter(ra -> ra.isActive() == active)
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "status = " + status;

                } else if (choice.equals("Assigned after date")) {
                    String date = ConsoleUtils.promptString(s, "Date (YYYY-MM-DD): ", true);

                    if (!DateUtils.isValidDateFormat(date)) {
                        ConsoleUtils.printError("Invalid date format");
                        return;
                    }

                    results = allAssignments.stream()
                            .filter(ra -> DateUtils.isAfter(ra.metadata().assignedAt().substring(0, 10), date))
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "assigned after " + date + " (" + DateUtils.formatRelativeTime(date) + ")";

                } else {
                    String date = ConsoleUtils.promptString(s, "Date (YYYY-MM-DD): ", true);

                    if (!DateUtils.isValidDateFormat(date)) {
                        ConsoleUtils.printError("Invalid date format");
                        return;
                    }

                    results = allAssignments.stream()
                            .filter(ra -> ra instanceof TemporaryAssignment)
                            .filter(ra -> DateUtils.isBefore(((TemporaryAssignment) ra).getExpiresAt(), date))
                            .collect(java.util.stream.Collectors.toList());
                    filterDesc = "expiring before " + date + " (" + DateUtils.formatRelativeTime(date) + ")";
                }

            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
                return;
            }

            if (results.isEmpty()) {
                ConsoleUtils.printInfo("No assignments found with filter: " + filterDesc);
            } else {
                ConsoleUtils.printSuccess("Search results for: " + filterDesc);

                String[] headers = {"USERNAME", "ROLE", "TYPE", "STATUS"};
                List<String[]> rows = new java.util.ArrayList<>();

                for (RoleAssignment ra : results) {
                    rows.add(new String[]{
                            ra.user().username(),
                            FormatUtils.truncate(ra.role().name(), 15),
                            ra.assignmentType(),
                            ra.isActive() ? "ACTIVE" : "INACTIVE"
                    });
                }

                System.out.println(FormatUtils.formatTable(headers, rows));
                ConsoleUtils.printSuccess("Total: " + results.size() + " assignments");
            }
        });

        //Команды просмотра прав:

        parser.registerCommand("permissions-user", "Show all permissions of a user", (s, sys) -> {
            ConsoleUtils.printHeader("USER PERMISSIONS");

            String username = ConsoleUtils.promptString(s, "Username: ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            var permissions = am.getUserPermissions(user);

            if (permissions.isEmpty()) {
                ConsoleUtils.printInfo("User '" + username + "' has no permissions.");
                return;
            }

            ConsoleUtils.printSuccess("Permissions for " + user.username() + " (" + user.fullName() + ")");

            String[] headers = {"RESOURCE", "ACTIONS"};
            List<String[]> rows = new java.util.ArrayList<>();

            java.util.Map<String, java.util.Set<String>> groupedByResource = new java.util.HashMap<>();

            for (Permission p : permissions) {
                groupedByResource.computeIfAbsent(p.resource(), k -> new java.util.HashSet<>()).add(p.name());
            }

            for (java.util.Map.Entry<String, java.util.Set<String>> entry : groupedByResource.entrySet()) {
                rows.add(new String[]{
                        entry.getKey(),
                        String.join(", ", entry.getValue())
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            ConsoleUtils.printSuccess("Total permissions: " + permissions.size());
        });

        parser.registerCommand("permissions-check", "Check if user has specific permission", (s, sys) -> {
            ConsoleUtils.printHeader("CHECK PERMISSION");

            String username = ConsoleUtils.promptString(s, "Username: ", true);
            String permName = ConsoleUtils.promptString(s, "Permission name (READ, WRITE, DELETE): ", true);
            String resource = ConsoleUtils.promptString(s, "Resource (users, reports, *): ", true);

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User '" + username + "' not found");
                return;
            }
            User user = userOpt.get();

            boolean hasPermission = am.userHasPermission(user, permName.toUpperCase(), resource.toLowerCase());

            System.out.println("\n" + "-".repeat(50));
            System.out.println("User: " + username);
            System.out.println("Permission: " + permName + " on " + resource);

            if (hasPermission) {
                ConsoleUtils.printSuccess("Result: HAS PERMISSION");
                System.out.println("\nGranted through roles:");
                List<RoleAssignment> assignments = am.findByUser(user);
                for (RoleAssignment ra : assignments) {
                    if (ra.isActive() && ra.role().hasPermission(permName.toUpperCase(), resource.toLowerCase())) {
                        System.out.println("  - " + ra.role().name() + " (" + ra.assignmentType() + ")");
                    }
                }
            } else {
                ConsoleUtils.printError("Result: NO PERMISSION");
            }
            System.out.println("-".repeat(50));
        });

        //Служебные команды:

        parser.registerCommand("help", "Show all commands", (s, sys) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Show system statistics", (s, sys) -> {
            System.out.println(FormatUtils.formatHeader("SYSTEM STATISTICS"));
            System.out.println(sys.generateStatistics());

            System.out.println(FormatUtils.formatHeader("DETAILED STATISTICS"));

            UserManager um = sys.getUserManager();
            AssignmentManager am = sys.getAssignmentManager();

            List<RoleAssignment> allAssignments = am.findAll();
            long activeAssignments = allAssignments.stream().filter(RoleAssignment::isActive).count();
            long expiredAssignments = allAssignments.size() - activeAssignments;

            String[] headers = {"METRIC", "VALUE"};
            List<String[]> rows = new java.util.ArrayList<>();
            rows.add(new String[]{"Total assignments", String.valueOf(allAssignments.size())});
            rows.add(new String[]{"Active assignments", String.valueOf(activeAssignments)});
            rows.add(new String[]{"Expired assignments", String.valueOf(expiredAssignments)});

            int userCount = um.count();
            if (userCount > 0) {
                double avgRolesPerUser = (double) allAssignments.size() / userCount;
                rows.add(new String[]{"Average roles per user", String.format("%.2f", avgRolesPerUser)});
            } else {
                rows.add(new String[]{"Average roles per user", "0"});
            }

            System.out.println(FormatUtils.formatTable(headers, rows));

            java.util.Map<String, Long> roleCount = new java.util.HashMap<>();
            for (RoleAssignment ra : allAssignments) {
                String roleName = ra.role().name();
                roleCount.put(roleName, roleCount.getOrDefault(roleName, 0L) + 1);
            }

            System.out.println(FormatUtils.formatHeader("Top 3 Most Popular Roles"));

            if (roleCount.isEmpty()) {
                System.out.println("  No roles assigned yet");
            } else {
                String[] roleHeaders = {"ROLE", "ASSIGNMENTS"};
                List<String[]> roleRows = new java.util.ArrayList<>();

                roleCount.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(3)
                        .forEach(e -> roleRows.add(new String[]{e.getKey(), String.valueOf(e.getValue())}));

                System.out.println(FormatUtils.formatTable(roleHeaders, roleRows));
            }
        });

        parser.registerCommand("clear", "Clear the screen", (s, sys) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit the program", (s, sys) -> {
            boolean confirmed = ConsoleUtils.promptYesNo(s, "Are you sure you want to exit?");

            if (confirmed) {
                ConsoleUtils.printSuccess("Goodbye!");
                System.exit(0);
            } else {
                ConsoleUtils.printInfo("Exit cancelled.");
            }
        });

        parser.registerCommand("save", "Save data to file", (s, sys) -> {
            ConsoleUtils.printHeader("SAVE DATA");

            String filename = ConsoleUtils.promptString(s, "Enter filename: ", false);
            if (filename == null || filename.isEmpty()) {
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

                ConsoleUtils.printSuccess("Data saved to '" + filename + "' successfully!");
            } catch (java.io.IOException e) {
                ConsoleUtils.printError("Error saving data: " + e.getMessage());
            }
        });

        parser.registerCommand("load", "Load data from file", (s, sys) -> {
            ConsoleUtils.printHeader("LOAD DATA");

            String filename = ConsoleUtils.promptString(s, "Enter filename: ", true);
            if (filename == null || filename.isEmpty()) {
                ConsoleUtils.printError("No filename provided");
                return;
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(filename))) {
                String line;
                String section = "";

                int usersLoaded = 0;
                int rolesLoaded = 0;
                int assignmentsLoaded = 0;
                int errorsSkipped = 0;

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
                                usersLoaded++;
                            } catch (IllegalArgumentException e) {
                                System.out.println("  Skipping user '" + parts[0] + "': " + e.getMessage());
                                errorsSkipped++;
                            }
                        }

                    } else if (section.equals("ROLES")) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 2) {
                            try {
                                Role role = new Role(parts[0], parts[1]);
                                for (int i = 2; i < parts.length; i++) {
                                    String[] permParts = parts[i].split(":");
                                    if (permParts.length >= 3) {
                                        try {
                                            role.addPermission(new Permission(permParts[0], permParts[1], permParts[2]));
                                        } catch (IllegalArgumentException e) {
                                            System.out.println("  Skipping permission '" + permParts[0] + "': " + e.getMessage());
                                            errorsSkipped++;
                                        }
                                    }
                                }
                                rm.add(role);
                                rolesLoaded++;
                            } catch (IllegalArgumentException e) {
                                System.out.println("  Skipping role '" + parts[0] + "': " + e.getMessage());
                                errorsSkipped++;
                            }
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
                                        assignmentsLoaded++;
                                    } else if (type.equals("TEMPORARY") && expiresAt != null) {
                                        TemporaryAssignment assignment = new TemporaryAssignment(userOpt.get(), roleOpt.get(), metadata, expiresAt, false);
                                        am.add(assignment);
                                        assignmentsLoaded++;
                                    }
                                } catch (IllegalStateException e) {
                                    System.out.println("  Skipping assignment '" + username + " -> " + roleName + "': " + e.getMessage());
                                    errorsSkipped++;
                                }
                            } else {
                                System.out.println("  Skipping assignment '" + username + " -> " + roleName + "': User or role not found");
                                errorsSkipped++;
                            }
                        }
                    }
                }

                ConsoleUtils.printSuccess("Data loaded from '" + filename + "' successfully!");
                System.out.println("  Users loaded: " + usersLoaded);
                System.out.println("  Roles loaded: " + rolesLoaded);
                System.out.println("  Assignments loaded: " + assignmentsLoaded);

                if (errorsSkipped > 0) {
                    ConsoleUtils.printInfo("Skipped " + errorsSkipped + " entries due to errors");
                }

            } catch (java.io.FileNotFoundException e) {
                ConsoleUtils.printError("File not found: " + filename);
            } catch (java.io.IOException e) {
                ConsoleUtils.printError("Error loading data: " + e.getMessage());
            }
        });


        parser.registerCommand("audit-log", "Show audit log", (s, sys) -> {
            ConsoleUtils.printHeader("AUDIT LOG");

            List<String> options = List.of(
                    "Show all entries",
                    "Filter by performer",
                    "Filter by action",
                    "Save to file"
            );

            String choice = ConsoleUtils.promptChoice(s, "Options:", options);
            AuditLog auditLog = sys.getAuditLog();

            if (choice.equals("Show all entries")) {
                List<AuditEntry> entries = auditLog.getAll();
                if (entries.isEmpty()) {
                    ConsoleUtils.printInfo("No audit entries found.");
                } else {
                    String[] headers = {"TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (AuditEntry entry : entries) {
                        rows.add(new String[]{
                                FormatUtils.truncate(entry.timestamp(), 19),
                                entry.action(),
                                entry.performer(),
                                FormatUtils.truncate(entry.target(), 20),
                                FormatUtils.truncate(entry.details(), 30)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                    ConsoleUtils.printSuccess("Total entries: " + entries.size());
                }

            } else if (choice.equals("Filter by performer")) {
                String performer = ConsoleUtils.promptString(s, "Enter performer username: ", true);
                var byPerformer = auditLog.getByPerformer(performer);
                if (byPerformer.isEmpty()) {
                    ConsoleUtils.printInfo("No entries found for performer: " + performer);
                } else {
                    String[] headers = {"TIMESTAMP", "ACTION", "TARGET", "DETAILS"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (AuditEntry entry : byPerformer) {
                        rows.add(new String[]{
                                FormatUtils.truncate(entry.timestamp(), 19),
                                entry.action(),
                                FormatUtils.truncate(entry.target(), 25),
                                FormatUtils.truncate(entry.details(), 30)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                    ConsoleUtils.printSuccess("Total: " + byPerformer.size());
                }

            } else if (choice.equals("Filter by action")) {
                String action = ConsoleUtils.promptString(s, "Enter action: ", true);
                var byAction = auditLog.getByAction(action.toUpperCase());
                if (byAction.isEmpty()) {
                    ConsoleUtils.printInfo("No entries found for action: " + action);
                } else {
                    String[] headers = {"TIMESTAMP", "PERFORMER", "TARGET", "DETAILS"};
                    List<String[]> rows = new java.util.ArrayList<>();

                    for (AuditEntry entry : byAction) {
                        rows.add(new String[]{
                                FormatUtils.truncate(entry.timestamp(), 19),
                                entry.performer(),
                                FormatUtils.truncate(entry.target(), 25),
                                FormatUtils.truncate(entry.details(), 35)
                        });
                    }
                    System.out.println(FormatUtils.formatTable(headers, rows));
                    ConsoleUtils.printSuccess("Total: " + byAction.size());
                }

            } else {
                String filename = ConsoleUtils.promptString(s, "Enter filename: ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "audit_log.txt";
                }
                auditLog.saveToFile(filename);
            }
        });

        parser.registerCommand("report-users", "Generate user report", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE USER REPORT");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateUserReport(sys.getUserManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: user_report.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "user_report.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });

        parser.registerCommand("report-roles", "Generate role report", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE ROLE REPORT");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateRoleReport(sys.getRoleManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: role_report.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "role_report.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });

        parser.registerCommand("report-matrix", "Generate permission matrix", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE PERMISSION MATRIX");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generatePermissionMatrix(sys.getUserManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: matrix_report.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "matrix_report.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });

        parser.registerCommand("report-users-parallel", "Generate user report (parallel)", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE USER REPORT (PARALLEL)");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateUserReportParallel(sys.getUserManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: user_report_parallel.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "user_report_parallel.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });


        parser.registerCommand("report-roles-parallel", "Generate role report (parallel)", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE ROLE REPORT (PARALLEL)");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateRoleReportParallel(sys.getRoleManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: role_report_parallel.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "role_report_parallel.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });


        parser.registerCommand("report-matrix-parallel", "Generate permission matrix (parallel)", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE PERMISSION MATRIX (PARALLEL)");

            ReportGenerator generator = new ReportGenerator();
            String report = generator.generatePermissionMatrixParallel(sys.getUserManager(), sys.getAssignmentManager());

            boolean saveToFile = ConsoleUtils.promptYesNo(s, "Save to file?");

            if (saveToFile) {
                String filename = ConsoleUtils.promptString(s, "Enter filename (default: matrix_report_parallel.txt): ", false);
                if (filename == null || filename.isEmpty()) {
                    filename = "matrix_report_parallel.txt";
                }
                generator.exportToFile(report, filename);
            } else {
                System.out.println(report);
            }
        });

        parser.registerCommand("report-users-async", "Generate user report (async)", (s, sys) -> {
            ConsoleUtils.printHeader("GENERATE USER REPORT (ASYNC)");

            ConsoleUtils.printInfo("Starting background task...");

            sys.getBackgroundExecutor().submit(() -> {
                try {
                    ReportGenerator generator = new ReportGenerator();
                    String report = generator.generateUserReport(sys.getUserManager(), sys.getAssignmentManager());

                    System.out.println(report);

                    ConsoleUtils.printSuccess("User report generation completed!");
                    sys.getAuditLog().log("REPORT_USERS_ASYNC", sys.getCurrentUser(), "system", "Async user report generated");

                } catch (Exception e) {
                    ConsoleUtils.printError("Error generating report: " + e.getMessage());
                }
                return null;
            });

            ConsoleUtils.printSuccess("Task submitted to background executor! Check later for results.");
        });

        parser.registerCommand("save-async", "Save data to file (async)", (s, sys) -> {
            ConsoleUtils.printHeader("SAVE DATA (ASYNC)");

            String filename = ConsoleUtils.promptString(s, "Enter filename (default: rbac_data_async.txt): ", false);
            if (filename == null || filename.isEmpty()) {
                filename = "rbac_data_async.txt";
            }

            final String finalFilename = filename;

            ConsoleUtils.printInfo("Starting background save to '" + finalFilename + "'...");

            sys.getBackgroundExecutor().submit(() -> {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(finalFilename))) {

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

                    ConsoleUtils.printSuccess("Data saved to '" + finalFilename + "' successfully!");
                    sys.getAuditLog().log("SAVE_ASYNC", sys.getCurrentUser(), finalFilename, "Async data save");

                } catch (java.io.IOException e) {
                    ConsoleUtils.printError("Error saving data: " + e.getMessage());
                    sys.getAuditLog().log("SAVE_ASYNC_ERROR", sys.getCurrentUser(), finalFilename, "Error: " + e.getMessage());
                }
                return null;
            });

            ConsoleUtils.printSuccess("Save task submitted to background executor!");
        });

        parser.registerCommand("save-async-status", "Check background executor status", (s, sys) -> {
            ConsoleUtils.printHeader("BACKGROUND EXECUTOR STATUS");

            ConsoleUtils.printInfo("Executor is ready for async tasks.");
            ConsoleUtils.printInfo("Active threads: " + Thread.getAllStackTraces().size());
            sys.getAuditLog().log("CHECK_ASYNC_STATUS", sys.getCurrentUser(), "system", "Status check");
        });

    }
}
