package com.recipemaster.recipeservice.integration;

import com.recipemaster.dto.UserDetailsDto;
import com.recipemaster.dto.UserDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Transactional
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testFindingUserEntityByEmailWhenUserExists() {
        Optional<UserEntity> result = userService.findUserEntityByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testFindingUserEntityByEmailWhenUserNotExists() {
        Optional<UserEntity> result = userService.findUserEntityByEmail("nonexistent@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindingByIdWhenUserExists() {
        UserDetailsDto result = userService.findById(testUser.getId());

        assertEquals(testUser.getId(), result.id());
        assertEquals("test@example.com", result.email());
    }

    @Test
    void testFindingByIdWhenUserNotExists() {
        Long nonExistentId = 999L;

        assertThrows(NoSuchElementException.class,
                () -> userService.findById(nonExistentId));
    }

    @Test
    void testFindingByEmailWhenUserExists() {
        UserDetailsDto result = userService.findByEmail("test@example.com");

        assertEquals(testUser.getId(), result.id());
        assertEquals("test@example.com", result.email());
    }

    @Test
    void testFindingByEmailWhenUserNotExists() {
        String nonExistentEmail = "nonexistent@example.com";

        assertThrows(NoSuchElementException.class,
                () -> userService.findByEmail(nonExistentEmail));
    }

    @Test
    void testLoadingUserByUsernameWhenUserExists() {
        UserDetails userDetails = userService.loadUserByUsername("test@example.com");

        assertEquals("test@example.com", userDetails.getUsername());
        assertTrue(passwordEncoder.matches("password123", userDetails.getPassword()));
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void testLoadingUserByUsernameWhenUserNotExists() {
        String nonExistentEmail = "nonexistent@example.com";

        Exception exception = assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(nonExistentEmail));

        assertEquals(nonExistentEmail, exception.getMessage());
    }

    @Test
    void testSuccessfulCreationOfNewUser() {
        UserDto newUser = new UserDto("new@example.com", "newpassword", "New User");

        userService.createNewUser(newUser);

        Optional<UserEntity> createdUser = userRepository.findByEmail("new@example.com");
        assertTrue(createdUser.isPresent());
        assertEquals("New User", createdUser.get().getFullName());
        assertTrue(passwordEncoder.matches("newpassword", createdUser.get().getPassword()));
    }

    @Test
    void testCreationOfNewUserWithExistingEmail() {
        UserDto existingUser = new UserDto("test@example.com", "password", "Test User");

        Exception exception = assertThrows(DataIntegrityViolationException.class,
                () -> userService.createNewUser(existingUser));

        assertNotNull(exception);
    }
}
