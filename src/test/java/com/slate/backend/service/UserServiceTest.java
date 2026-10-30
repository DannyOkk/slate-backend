package com.slate.backend.service;

import com.slate.backend.dto.ChangePasswordRequest;
import com.slate.backend.dto.UpdateProfileRequest;
import com.slate.backend.dto.UserProfileResponse;
import com.slate.backend.entity.User;
import com.slate.backend.exception.EmailAlreadyUsedException;
import com.slate.backend.exception.InvalidPasswordChangeException;
import com.slate.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Nombre original")
                .email("original@email.com")
                .phone("111")
                .password("stored-hash")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "original@email.com", null, java.util.Collections.emptyList()));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateCurrentUserProfile_updatesAllowedFieldsWithoutChangingPassword() {
        when(userRepository.findByEmail("original@email.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nuevo@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.updateCurrentUserProfile(
                new UpdateProfileRequest("Nuevo nombre", "nuevo@email.com", "+54 9 11"));

        assertEquals("Nuevo nombre", response.getName());
        assertEquals("nuevo@email.com", response.getEmail());
        assertEquals("+54 9 11", response.getPhone());
        assertEquals("stored-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUserProfile_rejectsDuplicateEmail() {
        User otherUser = User.builder().id(2L).email("nuevo@email.com").build();
        when(userRepository.findByEmail("original@email.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nuevo@email.com")).thenReturn(Optional.of(otherUser));

        assertThrows(EmailAlreadyUsedException.class, () -> userService.updateCurrentUserProfile(
                new UpdateProfileRequest("Nuevo nombre", "nuevo@email.com", null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeCurrentUserPassword_encodesAndSavesNewPassword() {
        when(userRepository.findByEmail("original@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("nueva123")).thenReturn("new-hash");

        userService.changeCurrentUserPassword(new ChangePasswordRequest("actual", "nueva123"));

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changeCurrentUserPassword_rejectsIncorrectCurrentPassword() {
        when(userRepository.findByEmail("original@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "stored-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.changeCurrentUserPassword(
                new ChangePasswordRequest("incorrecta", "nueva123")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeCurrentUserPassword_rejectsSamePassword() {
        when(userRepository.findByEmail("original@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual", "stored-hash")).thenReturn(true);

        assertThrows(InvalidPasswordChangeException.class, () -> userService.changeCurrentUserPassword(
                new ChangePasswordRequest("actual", "actual")));

        verify(userRepository, never()).save(any(User.class));
    }
}
