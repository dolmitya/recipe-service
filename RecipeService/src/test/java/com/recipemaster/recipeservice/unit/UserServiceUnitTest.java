package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.UserDetailsDto;
import com.recipemaster.dto.UserDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testFindingOfUserEntityByEmail() {
        String email = "test@example.com";
        UserEntity expectedUser = new UserEntity();
        expectedUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(expectedUser));

        Optional<UserEntity> result = userService.findUserEntityByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindingOfUserEntityByEmailWhenUserNotFound() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<UserEntity> result = userService.findUserEntityByEmail(email);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindingOfUserById() {
        Long userId = 1L;
        String email = "user@example.com";
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDetailsDto result = userService.findById(userId);

        assertEquals(userId, result.id());
        assertEquals(email, result.email());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindingOfUserByIdWhenUserNotFound() {
        Long userId = 99L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> userService.findById(userId));
        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId), exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void testFindingOfUserByEmail() {
        String email = "user@example.com";
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetailsDto result = userService.findByEmail(email);

        assertEquals(userId, result.id());
        assertEquals(email, result.email());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindingOfUserByEmailWhenUserNotFound() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> userService.findByEmail(email));
        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_EMAIL.getMessage(), exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testLoadingUserByUsername() {
        String email = "user@example.com";
        String password = "encodedPassword";
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(password);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(email);

        assertNotNull(result);
        assertEquals(email, result.getUsername());
        assertEquals(password, result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testLoadingUserByUsernameWhenUserNotFound() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(email));
        assertEquals(email, exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testCreationOfNewUser() {
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";
        UserDto inputDto = new UserDto("new@example.com", rawPassword, "New User");

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        userService.createNewUser(inputDto);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(UserEntity.class));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(inputDto.email(), savedUser.getEmail());
        assertEquals(inputDto.fullName(), savedUser.getFullName());
    }
}
