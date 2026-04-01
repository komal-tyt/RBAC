package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

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

        registerTestCommands();
    }

    private void registerTestCommands() {
        parser.registerCommand("help", "Show help", (s, sys) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Show statistics", (s, sys) -> {
            System.out.println(sys.generateStatistics());
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
        });

        parser.registerCommand("user-view", "View user", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            sys.getUserManager().findByUsername(username).ifPresentOrElse(
                    u -> System.out.println("FOUND: " + u.username()),
                    () -> System.out.println("NOT_FOUND")
            );
        });

        parser.registerCommand("user-delete", "Delete user", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Confirm (y/n): ");
            String confirm = s.nextLine().trim();
            if (confirm.equals("y")) {
                sys.getUserManager().findByUsername(username).ifPresent(sys.getUserManager()::remove);
                System.out.println("DELETED");
            }
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
        });

        parser.registerCommand("assign-role", "Assign role", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Role number: ");
            int roleNum = Integer.parseInt(s.nextLine().trim()) - 1;
            System.out.print("Type: ");
            String type = s.nextLine().trim();
            System.out.print("Reason: ");
            String reason = s.nextLine().trim();
            System.out.println("ASSIGNED");
        });

        parser.registerCommand("permissions-check", "Check permission", (s, sys) -> {
            System.out.print("Username: ");
            String username = s.nextLine().trim();
            System.out.print("Permission: ");
            String perm = s.nextLine().trim();
            System.out.print("Resource: ");
            String resource = s.nextLine().trim();
            System.out.println("RESULT");
        });

        parser.registerCommand("save", "Save data", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            System.out.println("SAVED");
        });

        parser.registerCommand("load", "Load data", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            System.out.println("LOADED");
        });

        parser.registerCommand("exit", "Exit", (s, sys) -> {
            System.out.print("Exit? (y/n): ");
            String answer = s.nextLine().trim();
            if (answer.equals("y")) System.out.println("EXITING");
            else System.out.println("CANCELLED");
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
    void testHelpCommand() {
        parser.parseAndExecute("help", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("COMMANDS:"));
    }

    @Test
    void testStatsCommand() {
        parser.parseAndExecute("stats", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("SYSTEM STATISTICS"));
        assertTrue(output.contains("Total users: 1"));
    }

    @Test
    void testClearCommand() {
        assertDoesNotThrow(() -> parser.parseAndExecute("clear", new Scanner(System.in), system));
    }

    @Test
    void testExitCommandCancelled() {
        Scanner mockScanner = createMockScanner("n\n");
        parser.parseAndExecute("exit", mockScanner, system);
        assertTrue(outputStream.toString().contains("CANCELLED"));
    }

    @Test
    void testExitCommandConfirmed() {
        Scanner mockScanner = createMockScanner("y\n");
        parser.parseAndExecute("exit", mockScanner, system);
        assertTrue(outputStream.toString().contains("EXITING"));
    }

    @Test
    void testUserCreateCommand() {
        Scanner mockScanner = createMockScanner("testuser\nTest User\ntest@example.com\n");
        parser.parseAndExecute("user-create", mockScanner, system);
        assertTrue(outputStream.toString().contains("SUCCESS"));
        assertTrue(system.getUserManager().exists("testuser"));
    }

    @Test
    void testUserCreateCommandWithInvalidData() {
        Scanner mockScanner = createMockScanner("ab\nInvalid User\ntest@example.com\n");
        parser.parseAndExecute("user-create", mockScanner, system);
        assertTrue(outputStream.toString().contains("ERROR"));
        assertFalse(system.getUserManager().exists("ab"));
    }

    @Test
    void testUserListCommand() {
        parser.parseAndExecute("user-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("COUNT: 1"));
    }

    @Test
    void testUserViewCommand() {
        Scanner mockScanner = createMockScanner("admin\n");
        parser.parseAndExecute("user-view", mockScanner, system);
        assertTrue(outputStream.toString().contains("FOUND: admin"));
    }

    @Test
    void testUserViewNonExistent() {
        Scanner mockScanner = createMockScanner("nonexistent\n");
        parser.parseAndExecute("user-view", mockScanner, system);
        assertTrue(outputStream.toString().contains("NOT_FOUND"));
    }

    @Test
    void testUserDeleteCommand() {
        User testUser = User.create("todelete", "To Delete", "delete@example.com");
        system.getUserManager().add(testUser);

        Scanner mockScanner = createMockScanner("todelete\ny\n");
        parser.parseAndExecute("user-delete", mockScanner, system);
        assertTrue(outputStream.toString().contains("DELETED"));
        assertFalse(system.getUserManager().exists("todelete"));
    }

    @Test
    void testRoleCreateCommand() {
        Scanner mockScanner = createMockScanner("TestRole\nTest Description\n");
        parser.parseAndExecute("role-create", mockScanner, system);
        assertTrue(outputStream.toString().contains("SUCCESS"));
        assertTrue(system.getRoleManager().exists("TestRole"));
    }

    @Test
    void testRoleListCommand() {
        parser.parseAndExecute("role-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("COUNT: 3"));
    }

    @Test
    void testAssignRoleCommand() {
        Scanner mockScanner = createMockScanner("admin\n1\npermanent\nTest reason\n");
        parser.parseAndExecute("assign-role", mockScanner, system);
        assertTrue(outputStream.toString().contains("ASSIGNED"));
    }

    @Test
    void testPermissionCheckCommand() {
        Scanner mockScanner = createMockScanner("admin\nREAD\nusers\n");
        parser.parseAndExecute("permissions-check", mockScanner, system);
        assertTrue(outputStream.toString().contains("RESULT"));
    }

    @Test
    void testSaveCommand() {
        Scanner mockScanner = createMockScanner("test_save.txt\n");
        parser.parseAndExecute("save", mockScanner, system);
        assertTrue(outputStream.toString().contains("SAVED"));
    }

    @Test
    void testLoadCommand() {
        Scanner mockScanner = createMockScanner("test_load.txt\n");
        parser.parseAndExecute("load", mockScanner, system);
        assertTrue(outputStream.toString().contains("LOADED"));
    }
}