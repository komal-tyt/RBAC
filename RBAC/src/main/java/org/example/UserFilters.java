package org.example;

public class UserFilters {

    static UserFilter byUsername(String username){
        return user -> user.username().equals(username);
    }

    static UserFilter byUsernameContains(String substring){
        return user -> user.username().toLowerCase().contains(substring.toLowerCase());
    }

    static UserFilter byEmail(String email){
        return user -> user.email().equals(email);
    }

    static UserFilter byEmailDomain(String domain){
        return user -> user.email().endsWith(domain);
    }

    static UserFilter byFullNameContains(String substring){
        return user -> user.fullName().contains(substring);
    }
}
