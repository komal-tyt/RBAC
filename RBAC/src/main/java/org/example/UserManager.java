package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager implements Repository<User> {

    private final Map<String, User> usersByUsername;
    private final ReentrantReadWriteLock lock;

    public UserManager() {
        this.usersByUsername = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void add(User item) {
        if (item == null) {
            throw new IllegalArgumentException("Error! User cannot be null");
        }

        lock.writeLock().lock();
        try {
            if (usersByUsername.containsKey(item.username())) {
                throw new IllegalArgumentException(
                        "User with username '" + item.username() + "' already exists"
                );
            }
            usersByUsername.put(item.username(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User item) {
        if (item == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            return usersByUsername.remove(item.username()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return findByUsername(id);
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(usersByUsername.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return usersByUsername.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            usersByUsername.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(usersByUsername.get(username));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            Stream<User> stream = usersByUsername.values().stream();

            if (filter != null) {
                stream = stream.filter(filter::test);
            }

            if (sorter != null) {
                stream = stream.sorted(sorter);
            }

            return stream.collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return usersByUsername.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String username, String newFullName, String newEmail) {
        lock.writeLock().lock();
        try {
            if (!usersByUsername.containsKey(username)) {
                throw new IllegalArgumentException("User not found: " + username);
            }

            User updated = User.create(username, newFullName, newEmail);
            usersByUsername.put(username, updated);
        } finally {
            lock.writeLock().unlock();
        }
    }
}