package org.example;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager implements Repository<User> {
    private final Map<String, User> usersByUsername;

    public UserManager(){
        this.usersByUsername = new HashMap<>();
    }

    @Override
    public List<User> findAll() {
        return usersByUsername.values().stream().collect(Collectors.toList());
    }

    @Override
    public int count() {
        return usersByUsername.size();
    }

    @Override
    public void add(User item) {
        if (item == null) {
            throw new IllegalArgumentException("Error! User cant null");
        }

        if (usersByUsername.containsKey(item.username())) {
            throw new IllegalArgumentException(
                    "User with username '" + item.username() + "' already exists"
            );
        }

        usersByUsername.put(item.username(), item);
    }

    @Override
    public boolean remove(User item) {
        if (item == null) {
            return false;
        }

        return usersByUsername.remove(item.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        return findByUsername(id);
    }

    @Override
    public void clear() {
        usersByUsername.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserManager  usermanager )) return false;
        return usersByUsername.equals(usermanager.usersByUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usersByUsername);
    }

    public Optional<User> findByUsername(String username){
        if (username == null || username.isBlank()){
            return Optional.empty();
        }

        return Optional.ofNullable(usersByUsername.get(username));
    }

    public Optional<User> findByEmail(String email){
        if (email == null || email.isBlank()){
            return Optional.empty();
        }

        return usersByUsername.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

    public List<User> findByFilter(UserFilter filter){
        if (filter == null){
            return new ArrayList<>(usersByUsername.values());
        }

        return usersByUsername.values().stream().filter(filter :: test).collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter){

        Stream<User> stream = usersByUsername.values().stream();

        if (filter != null){
            stream = stream.filter(filter::test);
        }

        if (sorter != null){
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String username){
        if (username == null) {
            return false;
        }

        return usersByUsername.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        if (!usersByUsername.containsKey(username)) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        User updated = User.create(username, newFullName, newEmail);
        usersByUsername.put(username, updated);
    }
}
