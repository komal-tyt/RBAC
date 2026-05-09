package org.example;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LoadTest {

    @Test
    void testLoadWithMultipleThreads() throws InterruptedException {
        System.out.println("\n========== LOAD TEST STARTED ==========");

        AuditLog auditLog = new AuditLog();
        RBACSystem system = new RBACSystem(auditLog);
        system.initialize();

        int threadCount = 10;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger totalUsersCreated = new AtomicInteger(0);
        AtomicInteger totalUsersUpdated = new AtomicInteger(0);
        AtomicInteger totalRolesCreated = new AtomicInteger(0);
        AtomicInteger totalAssignmentsCreated = new AtomicInteger(0);
        AtomicInteger totalSearches = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger duplicateUsers = new AtomicInteger(0);
        AtomicInteger duplicateRoles = new AtomicInteger(0);

        Set<String> createdUsernames = ConcurrentHashMap.newKeySet();
        Set<String> createdRoleNames = ConcurrentHashMap.newKeySet();

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadNum = t;
            executor.submit(() -> {
                Random random = new Random();

                for (int i = 0; i < operationsPerThread; i++) {
                    try {
                        int operation = random.nextInt(6);

                        switch (operation) {
                            case 0:
                                String username = "user" + threadNum + "_" + i + "_" + System.currentTimeMillis();
                                if (username.length() > 20) {
                                    username = username.substring(0, 20);
                                }
                                try {
                                    User user = User.create(username, "Test User " + threadNum, username + "@test.com");
                                    system.getUserManager().add(user);
                                    createdUsernames.add(username);
                                    totalUsersCreated.incrementAndGet();
                                } catch (IllegalArgumentException e) {
                                    duplicateUsers.incrementAndGet();
                                }
                                break;

                            case 1:
                                List<User> users = system.getUserManager().findAll();
                                if (!users.isEmpty()) {
                                    User randomUser = users.get(random.nextInt(users.size()));
                                    try {
                                        String newName = "Updated " + randomUser.fullName() + "_" + threadNum;
                                        system.getUserManager().update(randomUser.username(), newName, randomUser.email());
                                        totalUsersUpdated.incrementAndGet();
                                    } catch (IllegalArgumentException e) {
                                        errors.incrementAndGet();
                                    }
                                }
                                break;

                            case 2:
                                String roleName = "role_" + threadNum + "_" + i;
                                if (roleName.length() > 30) {
                                    roleName = roleName.substring(0, 30);
                                }
                                try {
                                    Role role = new Role(roleName, "Load test role " + threadNum);
                                    system.getRoleManager().add(role);
                                    createdRoleNames.add(roleName);
                                    totalRolesCreated.incrementAndGet();
                                } catch (IllegalArgumentException e) {
                                    duplicateRoles.incrementAndGet();
                                }
                                break;

                            case 3:
                                users = system.getUserManager().findAll();
                                List<Role> roles = system.getRoleManager().findAll();
                                if (!users.isEmpty() && !roles.isEmpty()) {
                                    User randomUser = users.get(random.nextInt(users.size()));
                                    Role randomRole = roles.get(random.nextInt(roles.size()));

                                    if (!system.getAssignmentManager().userHasRole(randomUser, randomRole)) {
                                        AssignmentMetadata meta = AssignmentMetadata.now("loadtest", "Load test assignment");
                                        String expiresAt = DateUtils.addDays(DateUtils.getCurrentDate(), 30);
                                        TemporaryAssignment assignment = new TemporaryAssignment(
                                                randomUser, randomRole, meta, expiresAt, false
                                        );
                                        system.getAssignmentManager().add(assignment);
                                        totalAssignmentsCreated.incrementAndGet();
                                    }
                                }
                                break;

                            case 4:
                                List<User> filtered = system.getUserManager().findByFilter(
                                        u -> u.username().contains("user") || u.email().contains("user")
                                );
                                totalSearches.incrementAndGet();
                                if (filtered == null) {
                                    errors.incrementAndGet();
                                }
                                break;

                            case 5:
                                List<Role> filteredRoles = system.getRoleManager().findByFilter(
                                        r -> r.name().contains("role")
                                );
                                totalSearches.incrementAndGet();
                                if (filteredRoles == null) {
                                    errors.incrementAndGet();
                                }
                                break;
                        }

                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.println("Unexpected error in thread " + threadNum + ": " + e.getMessage());
                    }
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(2, TimeUnit.MINUTES);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n========== LOAD TEST RESULTS ==========");
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Completed: " + completed);
        System.out.println("Users created: " + totalUsersCreated.get());
        System.out.println("Duplicate users attempts: " + duplicateUsers.get());
        System.out.println("Users updated: " + totalUsersUpdated.get());
        System.out.println("Roles created: " + totalRolesCreated.get());
        System.out.println("Duplicate roles attempts: " + duplicateRoles.get());
        System.out.println("Assignments created: " + totalAssignmentsCreated.get());
        System.out.println("Searches: " + totalSearches.get());
        System.out.println("Errors: " + errors.get());

        List<User> allUsers = system.getUserManager().findAll();
        Set<String> uniqueUsernames = new HashSet<>();
        int duplicates = 0;
        for (User u : allUsers) {
            if (!uniqueUsernames.add(u.username())) {
                duplicates++;
            }
        }
        System.out.println("Duplicate usernames in system: " + duplicates);

        assertTrue(completed);
        assertTrue(errors.get() < (threadCount * operationsPerThread) / 2);
        assertEquals(0, duplicates);

        System.out.println("========== LOAD TEST PASSED ==========");
        system.shutdown();
        Thread.sleep(500);
    }

    @Test
    void testConcurrentOperationsConsistency() throws InterruptedException {
        System.out.println("\n========== CONSISTENCY TEST STARTED ==========");

        AuditLog auditLog = new AuditLog();
        RBACSystem system = new RBACSystem(auditLog);
        system.initialize();

        User testUser = User.create("consistency_user", "Consistency Test", "consistency@test.com");
        system.getUserManager().add(testUser);

        Role testRole = new Role("TestRole", "Test role for consistency");
        system.getRoleManager().add(testRole);

        int threads = 20;
        int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        AtomicInteger operationsCompleted = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                Random random = new Random();

                for (int i = 0; i < operationsPerThread; i++) {
                    try {
                        if (random.nextBoolean()) {
                            if (!system.getAssignmentManager().userHasRole(testUser, testRole)) {
                                AssignmentMetadata meta = AssignmentMetadata.now("system", "test");
                                String expiresAt = DateUtils.addDays(DateUtils.getCurrentDate(), 30);
                                TemporaryAssignment assignment = new TemporaryAssignment(testUser, testRole, meta, expiresAt, false);
                                system.getAssignmentManager().add(assignment);
                            }
                        } else {
                            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(testUser);
                            for (RoleAssignment ra : assignments) {
                                if (ra.role().equals(testRole) && ra.isActive()) {
                                    if (ra instanceof TemporaryAssignment) {
                                        String yesterday = DateUtils.addDays(DateUtils.getCurrentDate(), -1);
                                        ((TemporaryAssignment) ra).extend(yesterday);
                                    } else if (ra instanceof PermanentAssignment) {
                                        ((PermanentAssignment) ra).revoke();
                                    }
                                }
                            }
                        }
                        operationsCompleted.incrementAndGet();

                    } catch (IllegalStateException e) {
                        System.out.println("DEBUG: " + e.getMessage());
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.println("Unexpected error: " + e.getMessage());
                    }
                }
                latch.countDown();
            });
        }

        latch.await(1, TimeUnit.MINUTES);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        boolean finalHasRole = system.getAssignmentManager().userHasRole(testUser, testRole);
        List<RoleAssignment> finalAssignments = system.getAssignmentManager().findByUser(testUser);

        boolean hasActiveAssignment = finalAssignments.stream()
                .anyMatch(ra -> ra.role().equals(testRole) && ra.isActive());

        System.out.println("\n========== CONSISTENCY TEST RESULTS ==========");
        System.out.println("Operations completed: " + operationsCompleted.get());
        System.out.println("Errors: " + errors.get());
        System.out.println("Final state - User has role: " + finalHasRole);
        System.out.println("Has active assignment: " + hasActiveAssignment);
        System.out.println("Total assignments for user: " + finalAssignments.size());

        if (finalHasRole) {
            assertTrue(hasActiveAssignment, "Has role but no active assignment!");
        }

        assertTrue(errors.get() < 100, "Too many errors: " + errors.get());

        System.out.println("========== CONSISTENCY TEST PASSED ==========");

        system.shutdown();
        Thread.sleep(500);
    }

    @Test
    void testRapidUserCreation() throws InterruptedException {
        System.out.println("\n========== RAPID USER CREATION TEST ==========");

        AuditLog auditLog = new AuditLog();
        RBACSystem system = new RBACSystem(auditLog);

        int threads = 10;
        int usersPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        Set<String> allUsernames = ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            final int threadNum = t;
            executor.submit(() -> {
                for (int i = 0; i < usersPerThread; i++) {
                    String username = "rapid_user_" + threadNum + "_" + i;
                    try {
                        User user = User.create(username, "Rapid Test User", username + "@test.com");
                        system.getUserManager().add(user);
                        allUsernames.add(username);
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        duplicateCount.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.println("Unexpected error: " + e.getMessage());
                    }
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long duration = System.currentTimeMillis() - startTime;

        int totalUsersInSystem = system.getUserManager().count();

        System.out.println("\n========== RAPID USER CREATION RESULTS ==========");
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Successfully created: " + successCount.get());
        System.out.println("Duplicate attempts: " + duplicateCount.get());
        System.out.println("Errors: " + errors.get());
        System.out.println("Unique usernames in set: " + allUsernames.size());
        System.out.println("Total users in system: " + totalUsersInSystem);

        assertEquals(successCount.get(), totalUsersInSystem,
                "Mismatch between created count and actual users in system");
        assertEquals(0, errors.get(), "Unexpected errors occurred");

        System.out.println("========== RAPID USER CREATION PASSED ==========");

        system.shutdown();
        Thread.sleep(500);
    }
}