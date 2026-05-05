package com.healthapp.service;

import com.healthapp.entity.User;
import com.healthapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    @Test
    void findOrCreateUserByGoogleInfo_createsUserAndNormalizesUsername() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john.doe+mobile@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("johndoemobile")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.findOrCreateUserByGoogleInfo(
                "google-1", "john.doe+mobile@example.com", "John", "Doe");

        assertEquals("johndoemobile", created.getUsername());
        assertEquals("google-1", created.getGoogleId());
        assertEquals(User.AccountStatus.ACTIVE, created.getAccountStatus());
        assertEquals(User.UserRole.USER, created.getRole());
    }

    @Test
    void changePassword_throwsWhenCurrentPasswordMismatch() {
        User user = new User();
        user.setId(11L);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.setPassword("storedHash");

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongCurrent", "storedHash")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.changePassword(11L, "wrongCurrent", "NewStrong1!"));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteUser_marksAccountDeleted() {
        User user = new User();
        user.setId(22L);
        user.setAccountStatus(User.AccountStatus.ACTIVE);

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(22L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(User.AccountStatus.DELETED, captor.getValue().getAccountStatus());
    }

    @Test
    void createUser_throwsWhenDuplicateEmail() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("taken@example.com");
        user.setPassword("Plain1!");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(user));
        assertTrue(ex.getMessage().contains("Email already exists"));
    }
}
