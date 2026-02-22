package org.example;

@FunctionalInterface
public interface UserFilter {
    boolean test(User user);

    public default UserFilter and(UserFilter other) {
        return user -> this.test(user) && other.test(user);
    }

    public default UserFilter or(UserFilter other) {
        return user -> this.test(user) || other.test(user);
    }
}
