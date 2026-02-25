package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager userManager;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        user1 = User.create("dima123", "Dima Kovalenko", "dima@example.com");
        user2 = User.create("nail666", "Nail Gadirov", "nail@example.com");
    }

    @Test
    void add_ShouldAddUser_WhenUserIsValid() {
        userManager.add(user1);

        assertEquals(1, userManager.count());
        assertTrue(userManager.exists("dima123"));
    }

    @Test
    void add_ShouldThrowException_WhenUserIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userManager.add(null)
        );
        assertEquals("Error! User cant null", exception.getMessage());
    }

    @Test
    void add_ShouldThrowException_WhenUsernameAlreadyExists() {
        userManager.add(user1);

        User duplicateUser = User.create("dima123", "Dima Kovalenko Copy", "dima_copy@example.com");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userManager.add(duplicateUser)
        );
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void remove_ShouldReturnTrue_WhenUserExists() {
        userManager.add(user1);

        boolean result = userManager.remove(user1);

        assertTrue(result);
        assertEquals(0, userManager.count());
    }

    @Test
    void remove_ShouldReturnFalse_WhenUserIsNull() {
        boolean result = userManager.remove(null);

        assertFalse(result);
    }

    @Test
    void remove_ShouldReturnFalse_WhenUserDoesNotExist() {
        boolean result = userManager.remove(user1);

        assertFalse(result);
    }

    @Test
    void findById_ShouldReturnUser_WhenUsernameExists() {
        userManager.add(user1);

        Optional<User> found = userManager.findById("dima123");

        assertTrue(found.isPresent());
        assertEquals("dima123", found.get().username());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenUsernameDoesNotExist() {
        Optional<User> found = userManager.findById("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenIdIsNull() {
        Optional<User> found = userManager.findById(null);

        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        userManager.add(user1);
        userManager.add(user2);

        List<User> allUsers = userManager.findAll();

        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(user1));
        assertTrue(allUsers.contains(user2));
    }

    @Test
    void count_ShouldReturnCorrectNumber() {
        assertEquals(0, userManager.count());

        userManager.add(user1);
        assertEquals(1, userManager.count());

        userManager.add(user2);
        assertEquals(2, userManager.count());
    }

    @Test
    void clear_ShouldRemoveAllUsers() {
        userManager.add(user1);
        userManager.add(user2);

        userManager.clear();

        assertEquals(0, userManager.count());
    }
    

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        userManager.add(user1);

        Optional<User> found = userManager.findByUsername("dima123");

        assertTrue(found.isPresent());
        assertEquals("dima123", found.get().username());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> found = userManager.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenUsernameIsNull() {
        Optional<User> found = userManager.findByUsername(null);

        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        userManager.add(user1);

        Optional<User> found = userManager.findByEmail("dima@example.com");

        assertTrue(found.isPresent());
        assertEquals("dima123", found.get().username());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> found = userManager.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailIsNull() {
        Optional<User> found = userManager.findByEmail(null);

        assertFalse(found.isPresent());
    }

    @Test
    void findByFilter_ShouldReturnFilteredUsers() {
        userManager.add(user1); // dima@example.com
        userManager.add(user2); // nail@example.com

        UserFilter filter = user -> user.email().endsWith("@example.com");
        List<User> result = userManager.findByFilter(filter);

        assertEquals(2, result.size());
    }

    @Test
    void findByFilter_ShouldReturnAllUsers_WhenFilterIsNull() {
        userManager.add(user1);
        userManager.add(user2);

        List<User> result = userManager.findByFilter(null);

        assertEquals(2, result.size());
    }

    @Test
    void findAll_WithFilterAndSorter_ShouldReturnFilteredAndSorted() {
        userManager.add(user1); // dima123
        userManager.add(user2); // nail666


        UserFilter filter = user -> user.username().contains("dima");

        Comparator<User> sorter = Comparator.comparing(User::username);

        List<User> result = userManager.findAll(filter, sorter);

        assertEquals(1, result.size());
        assertEquals("dima123", result.get(0).username());
    }

    @Test
    void exists_ShouldReturnTrue_WhenUserExists() {
        userManager.add(user1);

        assertTrue(userManager.exists("dima123"));
    }

    @Test
    void exists_ShouldReturnFalse_WhenUserDoesNotExist() {
        assertFalse(userManager.exists("nonexistent"));
    }

    @Test
    void exists_ShouldReturnFalse_WhenUsernameIsNull() {
        assertFalse(userManager.exists(null));
    }

    @Test
    void update_ShouldUpdateUserData_WhenUserExists() {
        userManager.add(user1);

        userManager.update("dima123", "Dima Updated", "dima_updated@example.com");

        Optional<User> updated = userManager.findByUsername("dima123");
        assertTrue(updated.isPresent());
        assertEquals("Dima Updated", updated.get().fullName());
        assertEquals("dima_updated@example.com", updated.get().email());
    }

    @Test
    void update_ShouldThrowException_WhenUserDoesNotExist() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userManager.update("nonexistent", "New Name", "new@email.com")
        );
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void equals_ShouldReturnTrue_WhenSameContent() {
        userManager.add(user1);

        UserManager anotherManager = new UserManager();
        anotherManager.add(user1);

        assertEquals(userManager, anotherManager);
    }
}