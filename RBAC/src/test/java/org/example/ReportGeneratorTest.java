package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private RBACSystem system;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        AuditLog auditLog = new AuditLog();
        system = new RBACSystem(auditLog);
        system.initialize();
        generator = new ReportGenerator();
    }

    @Test
    void testGenerateUserReport() {
        String report = generator.generateUserReport(system.getUserManager(), system.getAssignmentManager());

        assertTrue(report.contains("USER REPORT"));
        assertTrue(report.contains("admin"));
        assertTrue(report.contains("System Administrator"));
        assertTrue(report.contains("admin@example.com"));
        assertTrue(report.contains("Admin"));
    }

    @Test
    void testGenerateUserReportEmpty() {
        AuditLog auditLog = new AuditLog();
        RBACSystem emptySystem = new RBACSystem(auditLog);
        String report = generator.generateUserReport(emptySystem.getUserManager(), emptySystem.getAssignmentManager());
        assertTrue(report.contains("No users found"));
    }

    @Test
    void testGenerateRoleReport() {
        String report = generator.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());

        assertTrue(report.contains("ROLE REPORT"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Manager"));
        assertTrue(report.contains("Viewer"));
    }

    @Test
    void testGenerateRoleReportEmpty() {
        AuditLog auditLog = new AuditLog();
        RBACSystem emptySystem = new RBACSystem(auditLog);
        String report = generator.generateRoleReport(emptySystem.getRoleManager(), emptySystem.getAssignmentManager());
        assertTrue(report.contains("No roles found"));
    }

    @Test
    void testGeneratePermissionMatrix() {
        String matrix = generator.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());

        assertTrue(matrix.contains("PERMISSION MATRIX"));
        assertTrue(matrix.contains("admin"));
        assertTrue(matrix.contains("READ"));
        assertTrue(matrix.contains("WRITE"));
        assertTrue(matrix.contains("DELETE"));
    }

    @Test
    void testGeneratePermissionMatrixEmpty() {
        AuditLog auditLog = new AuditLog();
        RBACSystem emptySystem = new RBACSystem(auditLog);
        String matrix = generator.generatePermissionMatrix(emptySystem.getUserManager(), emptySystem.getAssignmentManager());
        assertTrue(matrix.contains("No users found"));
    }

    @Test
    void testExportToFile() throws IOException {
        String testReport = "Test report content";
        String testFile = "test_report.txt";

        generator.exportToFile(testReport, testFile);

        File file = new File(testFile);
        assertTrue(file.exists());

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String content = reader.readLine();
        reader.close();

        assertEquals(testReport, content);

        file.delete();
    }

    @Test
    void testExportToFileDefaultFilename() throws IOException {
        String testReport = "Test content";
        generator.exportToFile(testReport, null);

        File file = new File("report.txt");
        assertTrue(file.exists());
        file.delete();
    }
}