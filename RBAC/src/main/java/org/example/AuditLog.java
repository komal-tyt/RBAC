package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class AuditLog {

    private final List<AuditEntry> entries;
    private final BlockingQueue<AuditEntry> queue;
    private volatile boolean running;
    private Thread consumerThread;

    public AuditLog() {
        this.entries = new ArrayList<>();
        this.queue = new LinkedBlockingQueue<>();
        this.running = true;
        startConsumer();
    }


    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (running) {
                try {
                    // Блокирующее ожидание записи в очереди
                    AuditEntry entry = queue.take();
                    synchronized (entries) {
                        entries.add(entry);
                    }
                    System.out.println("[AUDIT] " + entry.format());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void log(String action, String performer, String target, String details) {
        AuditEntry entry = AuditEntry.create(action, performer, target, details);
        try {
            queue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Failed to queue audit entry: " + e.getMessage());
        }
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.isBlank()) {
            return new ArrayList<>();
        }
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.performer().equalsIgnoreCase(performer))
                    .collect(Collectors.toList());
        }
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.isBlank()) {
            return new ArrayList<>();
        }
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.action().equalsIgnoreCase(action))
                    .collect(Collectors.toList());
        }
    }

    public void printLog() {
        synchronized (entries) {
            if (entries.isEmpty()) {
                System.out.println("\n=== AUDIT LOG ===\nNo audit entries found.");
                return;
            }

            System.out.println("\n" + "=".repeat(90));
            System.out.println("AUDIT LOG");
            System.out.println("=".repeat(90));

            for (AuditEntry entry : entries) {
                System.out.println(entry.format());
            }

            System.out.println("=".repeat(90));
            System.out.println("Total entries: " + entries.size());
        }
    }

    public void saveToFile(String filename) {
        if (filename == null || filename.isBlank()) {
            filename = "audit_log.txt";
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== AUDIT LOG ===");
            writer.println("Generated at: " + java.time.LocalDateTime.now());
            writer.println("=".repeat(50));

            synchronized (entries) {
                for (AuditEntry entry : entries) {
                    writer.println(entry.format());
                }
            }

            writer.println("=".repeat(50));
            writer.println("Total entries: " + entries.size());

            System.out.println("Audit log saved to '" + filename + "' successfully!");
        } catch (IOException e) {
            System.out.println("Error saving audit log: " + e.getMessage());
        }
    }

    public void clear() {
        synchronized (entries) {
            entries.clear();
        }
        System.out.println("Audit log cleared.");
    }

    public void shutdown() {
        running = false;
        consumerThread.interrupt();
    }
}