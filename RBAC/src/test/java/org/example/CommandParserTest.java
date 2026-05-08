package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        AuditLog auditLog = new AuditLog();
        system = new RBACSystem(auditLog);
        system.initialize();

        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testRegisterCommand() {
        parser.registerCommand("test", "Test command", (s, sys) -> {
            System.out.println("Test executed");
        });

        parser.executeCommand("test", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("Test executed"));
    }

    @Test
    void testExecuteExistingCommand() {
        parser.registerCommand("hello", "Say hello", (s, sys) -> {
            System.out.println("Hello!");
        });

        parser.executeCommand("hello", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("Hello!"));
    }

    @Test
    void testExecuteNonExistingCommand() {
        parser.executeCommand("nonexistent", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("Command 'nonexistent' not found!"));
    }

    @Test
    void testPrintHelp() {
        parser.registerCommand("cmd1", "First command", (s, sys) -> {});
        parser.registerCommand("cmd2", "Second command", (s, sys) -> {});

        parser.printHelp();
        String output = outputStream.toString();
        assertTrue(output.contains("cmd1"));
        assertTrue(output.contains("First command"));
        assertTrue(output.contains("cmd2"));
        assertTrue(output.contains("Second command"));
    }

    @Test
    void testParseAndExecuteValidCommand() {
        parser.registerCommand("testcmd", "Test", (s, sys) -> {
            System.out.println("Command executed");
        });

        parser.parseAndExecute("testcmd", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("Command executed"));
    }

    @Test
    void testParseAndExecuteInvalidCommand() {
        parser.parseAndExecute("invalid", new Scanner(System.in), system);
        String output = outputStream.toString();
        assertTrue(output.contains("Unknown command: 'invalid'"));
    }

    @Test
    void testParseAndExecuteEmptyInput() {
        parser.parseAndExecute("", new Scanner(System.in), system);
        parser.parseAndExecute("   ", new Scanner(System.in), system);
        assertFalse(outputStream.toString().contains("Unknown command"));
    }

    @Test
    void testParseAndExecuteNullInput() {
        parser.parseAndExecute(null, new Scanner(System.in), system);
        assertFalse(outputStream.toString().contains("Unknown command"));
    }

    @Test
    void testParseAndExecuteCaseInsensitive() {
        parser.registerCommand("HELLO", "Say hello", (s, sys) -> {
            System.out.println("Hello!");
        });
        

        parser.parseAndExecute("hello", new Scanner(System.in), system);

        assertTrue(outputStream.toString().contains("Hello!"));
    }

    @Test
    void testParseAndExecuteWithSpaces() {
        parser.registerCommand("test", "Test", (s, sys) -> {
            System.out.println("Executed");
        });

        parser.parseAndExecute("   test   ", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("Executed"));
    }

    @Test
    void testCommandThrowsException() {
        parser.registerCommand("error", "Error", (s, sys) -> {
            throw new RuntimeException("Test error");
        });

        parser.parseAndExecute("error", new Scanner(System.in), system);
        assertTrue(outputStream.toString().contains("Error: Test error"));
    }
}