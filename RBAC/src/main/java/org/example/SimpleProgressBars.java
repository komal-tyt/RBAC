package org.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleProgressBars {

    private static final int THREAD_COUNT = 4;
    private static final int CALCULATION_STEPS = 30;
    private static final int STEP_DELAY_MS = 150;

    private static String[] displayLines;
    private static boolean[] completed;

    public static void main(String[] args) {
        displayLines = new String[THREAD_COUNT];
        completed = new boolean[THREAD_COUNT];

        System.out.println("\n========== МНОГОПОТОЧНЫЙ РАСЧЁТ ==========");
        System.out.println("Потоков: " + THREAD_COUNT + " | Шагов: " + CALCULATION_STEPS);
        System.out.println("===========================================\n");

        for (int i = 0; i < THREAD_COUNT; i++) {
            displayLines[i] = createInitialLine(i);
            System.out.println(displayLines[i]);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadNumber = i;
            executor.submit(() -> runCalculation(threadNumber));
        }

        executor.shutdown();
    }

    private static String createInitialLine(int threadNum) {
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < CALCULATION_STEPS; i++) {
            bar.append(" ");
        }
        bar.append("]");
        return String.format("Поток %d | [ID: ????] | %s 0%%",
                threadNum + 1, bar.toString());
    }

    private static void runCalculation(int threadNumber) {
        long threadId = Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();
        int displayIndex = threadNumber;

        for (int step = 1; step <= CALCULATION_STEPS; step++) {

            try {
                Thread.sleep(STEP_DELAY_MS);
            } catch (InterruptedException e) {
                return;
            }

            int percent = (step * 100) / CALCULATION_STEPS;
            String progressBar = buildProgressBar(step);

            String updatedLine = String.format("Поток %d | [ID: %d] | %s %3d%%",
                    threadNumber + 1, threadId, progressBar, percent);

            synchronized (displayLines) {
                displayLines[displayIndex] = updatedLine;
                redrawScreen();
            }
        }

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        String completionLine = String.format("Поток %d | [ID: %d] | ЗАВЕРШЁН | Время: %.2f сек",
                threadNumber + 1, threadId, duration);

        synchronized (displayLines) {
            displayLines[displayIndex] = completionLine;
            completed[displayIndex] = true;
            redrawScreen();
        }
    }

    private static String buildProgressBar(int currentStep) {
        StringBuilder bar = new StringBuilder("[");
        int filledCount = (currentStep * CALCULATION_STEPS) / CALCULATION_STEPS;

        for (int i = 0; i < CALCULATION_STEPS; i++) {
            if (i < filledCount) {
                bar.append("=");
            } else {
                bar.append("-");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    private static void redrawScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println("\n========== МНОГОПОТОЧНЫЙ РАСЧЁТ ==========");
        System.out.println("Потоков: " + THREAD_COUNT + " | Шагов: " + CALCULATION_STEPS);
        System.out.println("===========================================\n");

        for (int i = 0; i < THREAD_COUNT; i++) {
            System.out.println(displayLines[i]);
        }

        boolean allCompleted = true;
        for (boolean c : completed) {
            if (!c) allCompleted = false;
        }

        if (!allCompleted) {
            System.out.println("\nРасчёт выполняется...");
        } else {
            System.out.println("\nВСЕ ПОТОКИ ЗАВЕРШИЛИ РАБОТУ!");
        }
    }
}
