package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.UserDetailsDto;
import com.recipemaster.dto.UserDto;
import com.recipemaster.dto.UserProfileDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("demo@recipe-service.local");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Demo User");
    }

    @Test
    void findUserEntityByEmail_WhenUserExists() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        Optional<UserEntity> result = userService.findUserEntityByEmail(testUser.getEmail());

        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void findUserEntityByEmail_WhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        Optional<UserEntity> result = userService.findUserEntityByEmail("missing@example.com");

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void findById_WhenUserExists() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserDetailsDto result = userService.findById(testUser.getId());

        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getEmail(), result.email());
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    void findById_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> userService.findById(99L)
        );

        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(99L), exception.getMessage());
        verify(userRepository).findById(99L);
    }

    @Test
    void findByEmail_WhenUserExists() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserDetailsDto result = userService.findByEmail(testUser.getEmail());

        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getEmail(), result.email());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void findByEmail_WhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> userService.findByEmail("missing@example.com")
        );

        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_EMAIL.getMessage(), exception.getMessage());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void getProfileByEmail_WhenUserExists() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserProfileDto result = userService.getProfileByEmail(testUser.getEmail());

        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getEmail(), result.email());
        assertEquals(testUser.getFullName(), result.fullName());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void getProfileByEmail_WhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getProfileByEmail("missing@example.com"));
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void loadUserByUsername_WhenUserExists() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        UserDetails result = userService.loadUserByUsername(testUser.getEmail());

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getUsername());
        assertEquals(testUser.getPassword(), result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void loadUserByUsername_WhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("missing@example.com")
        );

        assertEquals("missing@example.com", exception.getMessage());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void createNewUser_WhenInputIsValid() {
        UserDto userDto = new UserDto("new@example.com", "plainPassword", "New User");
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createNewUser(userDto);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(userDto.email(), captor.getValue().getEmail());
        assertEquals("encodedPassword", captor.getValue().getPassword());
        assertEquals(userDto.fullName(), captor.getValue().getFullName());
        verify(passwordEncoder).encode("plainPassword");
    }
}
