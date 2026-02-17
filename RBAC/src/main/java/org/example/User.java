package org.example;



public record User (String username, String fullName, String email) {

    public static User create(String username, String fullName, String email){

        if (username == null || username.isEmpty()){
            throw new IllegalArgumentException("Username is a required field");
        }

        if (fullName == null || fullName.isEmpty()){
            throw new IllegalArgumentException("FullName is a required field");
        }

        if (email == null || email.isEmpty()){
            throw new IllegalArgumentException("Email is a required field");
        }

        if ((username.length() < 3) || (username.length() > 20)){
            throw new IllegalArgumentException("Username from 3 to 20 characters");
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")){
            throw new IllegalArgumentException("The username must contain only Latin letters, numbers, and underscores.");
        }

        if (!email.matches("^.+@.+\\..+$")){
            throw new IllegalArgumentException("Email must contain @ and a dot after @");
        }





        return new User(username, fullName, email);
    }

    public String format(){
        return (this.username + " (" + this.fullName +") " + " <" + email +"> ");
    }

}
