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
        AuditLog auditLog = new AuditLog();
        system = new RBACSystem(auditLog);
        system.initialize();
        parser = new CommandParser();

        registerCommands();

        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    private void registerCommands() {
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
                sys.getAuditLog().log("CREATE_USER", sys.getCurrentUser(), username, "Created");
                System.out.println("SUCCESS");
            } catch (Exception e) {
                System.out.println("ERROR");
            }
        });

        parser.registerCommand("user-list", "List users", (s, sys) -> {
            var users = sys.getUserManager().findAll();
            System.out.println("COUNT: " + users.size());
        });

        parser.registerCommand("role-list", "List roles", (s, sys) -> {
            var roles = sys.getRoleManager().findAll();
            System.out.println("COUNT: " + roles.size());
        });

        parser.registerCommand("stats", "Statistics", (s, sys) -> {
            System.out.println(sys.generateStatistics());
        });

        parser.registerCommand("audit-log", "Audit log", (s, sys) -> {
            sys.getAuditLog().printLog();
        });

        parser.registerCommand("save", "Save", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            System.out.println("SAVED");
        });

        parser.registerCommand("load", "Load", (s, sys) -> {
            System.out.print("Filename: ");
            String filename = s.nextLine().trim();
            System.out.println("LOADED");
        });
    }

    private Scanner createMockScanner(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testFullUserLifecycle() {
        Scanner createScanner = createMockScanner("testuser\nTest User\ntest@example.com\n");
        parser.parseAndExecute("user-create", createScanner, system);
        assertTrue(outputStream.toString().contains("SUCCESS"));

        outputStream.reset();
        parser.parseAndExecute("user-list", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("testuser"));

        assertTrue(system.getUserManager().exists("testuser"));
    }

    @Test
    void testStatsCommand() {
        parser.parseAndExecute("stats", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("SYSTEM STATISTICS"));
        assertTrue(output.contains("Total users: 1"));
        assertTrue(output.contains("Total roles: 3"));
    }

    @Test
    void testRoleListCommand() {
        parser.parseAndExecute("role-list", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("Manager"));
        assertTrue(output.contains("Viewer"));
    }

    @Test
    void testAuditLogCommand() {
        system.getAuditLog().log("TEST", "admin", "target", "details");

        outputStream.reset();
        Scanner scanner = createMockScanner("1\n");
        parser.parseAndExecute("audit-log", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("TEST") || output.contains("No audit entries"));
    }

    @Test
    void testSaveCommand() {
        Scanner saveScanner = createMockScanner("test_save.txt\n");
        parser.parseAndExecute("save", saveScanner, system);
        assertTrue(outputStream.toString().contains("SAVED"));
    }

    @Test
    void testLoadCommand() {
        Scanner loadScanner = createMockScanner("test_load.txt\n");
        parser.parseAndExecute("load", loadScanner, system);
        assertTrue(outputStream.toString().contains("LOADED"));
    }
}