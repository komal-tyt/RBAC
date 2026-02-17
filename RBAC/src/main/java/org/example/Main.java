package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        System.out.println("ТЕСТ 1");
        try {
            User user1 = User.create("dima", "kovalenko", "dima@.example.com");
            System.out.println("User create!!!");
        } catch (IllegalArgumentException e){
            System.out.println("Error!");
        }
        System.out.println();

        System.out.println("ТЕСТ 2");
        try {
            User user2 = User.create("", "gadirov", "nail@example.com");
            System.out.println("Error!");
        } catch (IllegalArgumentException e) {
            System.out.println("User not create!!!!");
        }
        System.out.println();

        System.out.println("ТЕСТ 3");
        try {
            User user4 = User.create("na", "gadirov", "nail@example.com");
            System.out.println("Error!");
        } catch (IllegalArgumentException e) {
            System.out.println("Username small!!");
        }
        System.out.println();

        System.out.println("ТЕСТ 4");
        try {
            User user6 = User.create("дима123", "kovalenko", "dima@example.com");
            System.out.println("Error!");
        } catch (IllegalArgumentException e) {
            System.out.println("User not create!!!");
        }
        System.out.println();

        System.out.println("ТЕСТ 5");
        try {
            User user12 = User.create("nail123", "gadirov", "nailnexample.com");
            System.out.println("Error!");
        } catch (IllegalArgumentException e) {
            System.out.println("Email is not correct!");
        }
        System.out.println();


    }
}
