package org.example;



public record User (String username, String fullName, String email) {

    public static User create(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(fullName, "FullName");
        ValidationUtils.requireNonEmpty(email, "Email");

        if (!ValidationUtils.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be 3-20 characters long and contain only letters, numbers, and underscores");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Email must contain @ and a dot after @");
        }

        return new User(username, fullName, email);
    }

    public String format(){
        return (this.username + " (" + this.fullName +") " + " <" + email +"> ");
    }

}
