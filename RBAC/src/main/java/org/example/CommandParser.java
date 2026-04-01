package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private Map<String, Command> commands;
    private Map<String, String> commandDescriptions;

    public CommandParser(){
        this.commands = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        String lowerName = name.toLowerCase();
        commands.put(lowerName, command);
        commandDescriptions.put(lowerName, description);
    }

    void executeCommand(String commandName, Scanner scanner, RBACSystem system){
        Command command = commands.get(commandName);

        if (command != null) {
            command.execute(scanner, system);
        } else {
            System.out.println("Command '" + commandName + "' not found!");
        }
    }

    void printHelp(){
        System.out.println("\nCOMMANDS:");

        for (String key : commandDescriptions.keySet()) {
            System.out.println("  " + key + " - " + commandDescriptions.get(key));
        }
    }

    void parseAndExecute(String input, Scanner scanner, RBACSystem system){
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0].toLowerCase();

        Command command = commands.get(commandName);

        if (command != null) {
            try {
                command.execute(scanner, system);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Unknown command: '" + commandName + "'");
            System.out.println("Type 'help' to see available commands.");
        }
    }

}
