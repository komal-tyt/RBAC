package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    private AuditLog auditLog;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testLog() {
        auditLog.log("CREATE_USER", "admin", "john", "test details");
        assertEquals(1, auditLog.getAll().size());
        assertTrue(outputStream.toString().contains("[AUDIT]"));
    }

    @Test
    void testGetAll() {
        auditLog.log("ACTION1", "admin", "target1", "details1");
        auditLog.log("ACTION2", "admin", "target2", "details2");
        assertEquals(2, auditLog.getAll().size());
    }

    @Test
    void testGetByPerformer() {
        auditLog.log("ACTION1", "admin", "target1", "details");
        auditLog.log("ACTION2", "admin", "target2", "details");
        auditLog.log("ACTION3", "user", "target3", "details");

        assertEquals(2, auditLog.getByPerformer("admin").size());
        assertEquals(1, auditLog.getByPerformer("user").size());
        assertEquals(0, auditLog.getByPerformer("nonexistent").size());
    }

    @Test
    void testGetByAction() {
        auditLog.log("CREATE_USER", "admin", "john", "details");
        auditLog.log("DELETE_USER", "admin", "john", "details");
        auditLog.log("CREATE_USER", "admin", "jane", "details");

        assertEquals(2, auditLog.getByAction("CREATE_USER").size());
        assertEquals(1, auditLog.getByAction("DELETE_USER").size());
        assertEquals(0, auditLog.getByAction("UNKNOWN").size());
    }

    @Test
    void testPrintLogWithEntries() {
        auditLog.log("CREATE_USER", "admin", "john", "test");
        auditLog.printLog();
        String output = outputStream.toString();
        assertTrue(output.contains("AUDIT LOG"));
        assertTrue(output.contains("CREATE_USER"));
        assertTrue(output.contains("admin"));
    }

    @Test
    void testPrintLogEmpty() {
        auditLog.printLog();
        String output = outputStream.toString();
        assertTrue(output.contains("No audit entries found"));
    }

    @Test
    void testSaveToFile() throws IOException {
        auditLog.log("TEST_ACTION", "admin", "target", "details");

        String testFile = "test_audit_log.txt";
        auditLog.saveToFile(testFile);

        File file = new File(testFile);
        assertTrue(file.exists());

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String content = reader.lines().reduce("", (a, b) -> a + b);
        reader.close();

        assertTrue(content.contains("TEST_ACTION"));

        file.delete();
    }

    @Test
    void testClear() {
        auditLog.log("ACTION1", "admin", "target", "details");
        assertEquals(1, auditLog.getAll().size());
        auditLog.clear();
        assertEquals(0, auditLog.getAll().size());
        assertTrue(outputStream.toString().contains("Audit log cleared"));
    }
}