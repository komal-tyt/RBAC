package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private RBACSystem system;
    private CommandParser parser;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
        parser = new CommandParser();

        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        registerAllCommands();
    }

    private void registerAllCommands() {
        parser.registerCommand("help", "Show help", (s, sys) -> parser.printHelp());

        parser.registerCommand("stats", "Show statistics", (s, sys) -> {
            System.out.println(sys.generateStatistics());
            System.out.println("\n=== DETAILED STATISTICS ===");
            var allAssignments = sys.getAssignmentManager().findAll();
            long active = allAssignments.stream().filter(RoleAssignment::isActive).count();
            System.out.println("Active assignments: " + active);
            System.out.println("Expired assignments: " + (allAssignments.size() - active));
            if (sys.getUserManager().count() > 0) {
                double avg = (double) allAssignments.size() / sys.getUserManager().count();
                System.out.printf("Average roles per user: %.2f\n", avg);
            }
        });

        parser.registerCommand("clear", "Clear screen", (s, sys) -> {
            for (int i = 0; i < 10; i++) System.out.println();
        });

        parser.registerCommand("user-create", "Create user", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Full Name: ");
            String fullName = s.nextLine().trim();
            System.out.print("Email: ");
            String email = s.nextLine().trim();
            try {
                User user = User.create(username, fullName, email);
                sys.getUserManager().add(user);
                System.out.println("SUCCESS");
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        });

        parser.registerCommand("user-list", "List users", (s, sys) -> {
            var users = sys.getUserManager().findAll();
            System.out.println("COUNT: " + users.size());
            for (User u : users) {
                System.out.println("  " + u.username());
            }
        });

        parser.registerCommand("user-view", "View user", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            sys.getUserManager().findByUsername(username).ifPresentOrElse(
                    u -> {
                        System.out.println("FOUND: " + u.username());
                        System.out.println("  Full Name: " + u.fullName());
                        System.out.println("  Email: " + u.email());
                        var assignments = sys.getAssignmentManager().findByUser(u);
                        System.out.println("  Roles: " + assignments.size());
                    },
                    () -> System.out.println("NOT_FOUND")
            );
        });

        parser.registerCommand("user-delete", "Delete user", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Confirm (y/n): ");
            String confirm = s.nextLine().trim();
            if (confirm.equals("y")) {
                sys.getUserManager().findByUsername(username).ifPresent(user -> {
                    var assignments = sys.getAssignmentManager().findByUser(user);
                    assignments.forEach(sys.getAssignmentManager()::remove);
                    sys.getUserManager().remove(user);
                    System.out.println("DELETED");
                });
            }
        });

        parser.registerCommand("user-search", "Search users", (s, sys) -> {
            System.out.println("1. By username");
            System.out.println("2. By email");
            System.out.print("Choice: ");
            String choice = s.nextLine().trim();
            System.out.print("Enter term: ");
            String term = s.nextLine().trim().toLowerCase();
            var results = sys.getUserManager().findAll().stream()
                    .filter(u -> choice.equals("1") ? u.username().toLowerCase().contains(term) : u.email().toLowerCase().contains(term))
                    .toList();
            System.out.println("RESULTS: " + results.size());
        });

        parser.registerCommand("role-create", "Create role", (s, sys) -> {
            System.out.print("Role name: ");
            String name = s.nextLine().trim();
            System.out.print("Description: ");
            String desc = s.nextLine().trim();
            Role role = new Role(name, desc);
            sys.getRoleManager().add(role);
            System.out.println("SUCCESS");
        });

        parser.registerCommand("role-list", "List roles", (s, sys) -> {
            var roles = sys.getRoleManager().findAll();
            System.out.println("COUNT: " + roles.size());
            for (Role r : roles) {
                System.out.println("  " + r.name() + " (" + r.getPermissions().size() + " permissions)");
            }
        });

        parser.registerCommand("role-view", "View role", (s, sys) -> {
            System.out.print("Role name: ");
            String roleName = s.nextLine().trim();
            sys.getRoleManager().findByName(roleName).ifPresentOrElse(
                    r -> System.out.println("FOUND: " + r.name() + "\n" + r.format()),
                    () -> System.out.println("NOT_FOUND")
            );
        });

        parser.registerCommand("assign-role", "Assign role", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }
            var roles = sys.getRoleManager().findAll();
            System.out.println("Available roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + roles.get(i).name());
            }
            System.out.print("Select role: ");
            int idx = Integer.parseInt(s.nextLine().trim()) - 1;
            System.out.print("Type (permanent/temporary): ");
            String type = s.nextLine().trim();
            System.out.print("Reason: ");
            String reason = s.nextLine().trim();
            AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), reason);
            if (type.equals("permanent")) {
                sys.getAssignmentManager().add(new PermanentAssignment(userOpt.get(), roles.get(idx), meta));
                System.out.println("ASSIGNED");
            } else {
                System.out.print("Expiration date: ");
                String expires = s.nextLine().trim();
                sys.getAssignmentManager().add(new TemporaryAssignment(userOpt.get(), roles.get(idx), meta, expires, false));
                System.out.println("ASSIGNED");
            }
        });

        parser.registerCommand("assignment-list", "List assignments", (s, sys) -> {
            var assignments = sys.getAssignmentManager().findAll();
            System.out.println("COUNT: " + assignments.size());
            for (RoleAssignment ra : assignments) {
                System.out.println("  " + ra.user().username() + " -> " + ra.role().name() + " (" + ra.assignmentType() + ")");
            }
        });

        parser.registerCommand("permissions-check", "Check permission", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Permission: ");
            String permName = s.nextLine().trim().toUpperCase();
            System.out.print("Resource: ");
            String resource = s.nextLine().trim().toLowerCase();
            var userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }
            boolean has = sys.getAssignmentManager().userHasPermission(userOpt.get(), permName, resource);
            System.out.println("RESULT: " + (has ? "YES" : "NO"));
        });

        parser.registerCommand("save", "Save data", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            if (filename.isEmpty()) filename = "test_data.txt";
            try (PrintWriter w = new PrintWriter(new FileWriter(filename))) {
                w.println("[USERS]");
                for (User u : sys.getUserManager().findAll()) {
                    w.println(u.username() + "|" + u.fullName() + "|" + u.email());
                }
                w.println("\n[ROLES]");
                for (Role r : sys.getRoleManager().findAll()) {
                    w.print(r.name() + "|" + r.getDescription());
                    for (Permission p : r.getPermissions()) {
                        w.print("|" + p.name() + ":" + p.resource() + ":" + p.description());
                    }
                    w.println();
                }
                System.out.println("SAVED");
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        });

        parser.registerCommand("load", "Load data", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
                String line;
                String section = "";
                sys.getUserManager().clear();
                sys.getRoleManager().clear();
                sys.getAssignmentManager().clear();
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.equals("[USERS]")) { section = "USERS"; continue; }
                    if (line.equals("[ROLES]")) { section = "ROLES"; continue; }
                    if (section.equals("USERS")) {
                        String[] p = line.split("\\|");
                        if (p.length >= 3) {
                            try {
                                sys.getUserManager().add(User.create(p[0], p[1], p[2]));
                            } catch (Exception e) {}
                        }
                    } else if (section.equals("ROLES")) {
                        String[] p = line.split("\\|");
                        if (p.length >= 2) {
                            Role role = new Role(p[0], p[1]);
                            for (int i = 2; i < p.length; i++) {
                                String[] perm = p[i].split(":");
                                if (perm.length >= 3) {
                                    role.addPermission(new Permission(perm[0], perm[1], perm[2]));
                                }
                            }
                            sys.getRoleManager().add(role);
                        }
                    }
                }
                System.out.println("LOADED");
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        });
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private Scanner createMockScanner(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void testFullUserLifecycle() {
        Scanner createScanner = createMockScanner("intuser\nIntegration User\nint@test.com\n");
        parser.parseAndExecute("user-create", createScanner, system);
        assertTrue(outputStream.toString().contains("SUCCESS"));

        outputStream.reset();
        parser.parseAndExecute("user-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("intuser"));

        outputStream.reset();
        Scanner viewScanner = createMockScanner("intuser\n");
        parser.parseAndExecute("user-view", viewScanner, system);
        assertTrue(outputStream.toString().contains("FOUND: intuser"));

        outputStream.reset();
        Scanner deleteScanner = createMockScanner("intuser\ny\n");
        parser.parseAndExecute("user-delete", deleteScanner, system);
        assertTrue(outputStream.toString().contains("DELETED"));

        outputStream.reset();
        parser.parseAndExecute("user-list", new Scanner(System.in), system);
        assertFalse(outputStream.toString().contains("intuser"));
    }

    @Test
    void testFullRoleLifecycle() {
        Scanner createScanner = createMockScanner("IntRole\nTest Description\n");
        parser.parseAndExecute("role-create", createScanner, system);
        assertTrue(outputStream.toString().contains("SUCCESS"));

        outputStream.reset();
        parser.parseAndExecute("role-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("IntRole"));

        outputStream.reset();
        Scanner viewScanner = createMockScanner("IntRole\n");
        parser.parseAndExecute("role-view", viewScanner, system);
        assertTrue(outputStream.toString().contains("FOUND: IntRole"));
    }

    @Test
    void testFullAssignmentLifecycle() {
        Scanner createUser = createMockScanner("assignint\nAssign Int\nassign@test.com\n");
        parser.parseAndExecute("user-create", createUser, system);

        outputStream.reset();
        Scanner assignScanner = createMockScanner("assignint\n3\npermanent\nIntegration test\n");
        parser.parseAndExecute("assign-role", assignScanner, system);
        assertTrue(outputStream.toString().contains("ASSIGNED"));

        outputStream.reset();
        parser.parseAndExecute("assignment-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("assignint -> Viewer"));
    }

    @Test
    void testStatsCommandIntegration() {
        parser.parseAndExecute("stats", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("SYSTEM STATISTICS"));
        assertTrue(output.contains("Total users: 1"));
        assertTrue(output.contains("Total roles: 3"));
        assertTrue(output.contains("Active assignments: 1"));
    }

    @Test
    void testHelpCommandIntegration() {
        parser.parseAndExecute("help", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("COMMANDS:"));
    }

    @Test
    void testPermissionCheckIntegration() {
        Scanner checkScanner = createMockScanner("admin\nREAD\nusers\n");
        parser.parseAndExecute("permissions-check", checkScanner, system);
        assertTrue(outputStream.toString().contains("RESULT: YES"));
    }

    @Test
    void testUserSearchIntegration() {
        Scanner createScanner = createMockScanner("searchint\nSearch Int\nsearch@test.com\n");
        parser.parseAndExecute("user-create", createScanner, system);

        outputStream.reset();
        Scanner searchScanner = createMockScanner("1\nsearch\n");
        parser.parseAndExecute("user-search", searchScanner, system);
        assertTrue(outputStream.toString().contains("RESULTS: 1"));
    }

    @Test
    void testSaveAndLoadIntegration() throws IOException {
        Scanner createUser = createMockScanner("saveint\nSave Int\nsave@test.com\n");
        parser.parseAndExecute("user-create", createUser, system);

        String testFile = "integration_test_data.txt";
        Scanner saveScanner = createMockScanner(testFile + "\n");
        parser.parseAndExecute("save", saveScanner, system);
        assertTrue(outputStream.toString().contains("SAVED"));

        RBACSystem newSystem = new RBACSystem();
        CommandParser newParser = new CommandParser();
        ByteArrayOutputStream newOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(newOutput));

        registerAllCommandsToParser(newParser);

        Scanner loadScanner = createMockScanner(testFile + "\n");
        newParser.parseAndExecute("load", loadScanner, newSystem);
        assertTrue(newOutput.toString().contains("LOADED"));

        new File(testFile).delete();
        System.setOut(originalOut);
    }

    private void registerAllCommandsToParser(CommandParser p) {
        p.registerCommand("load", "Load", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
                String line;
                String section = "";
                sys.getUserManager().clear();
                sys.getRoleManager().clear();
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.equals("[USERS]")) { section = "USERS"; continue; }
                    if (line.equals("[ROLES]")) { section = "ROLES"; continue; }
                    if (section.equals("USERS")) {
                        String[] p2 = line.split("\\|");
                        if (p2.length >= 3) {
                            try {
                                sys.getUserManager().add(User.create(p2[0], p2[1], p2[2]));
                            } catch (Exception e) {}
                        }
                    } else if (section.equals("ROLES")) {
                        String[] p2 = line.split("\\|");
                        if (p2.length >= 2) {
                            Role role = new Role(p2[0], p2[1]);
                            sys.getRoleManager().add(role);
                        }
                    }
                }
                System.out.println("LOADED");
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        });
    }
}