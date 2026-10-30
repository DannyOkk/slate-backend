package com.slate.backend.service;

import com.slate.backend.dto.ChangePasswordRequest;
import com.slate.backend.dto.UpdateProfileRequest;
import com.slate.backend.dto.UserProfileResponse;
import com.slate.backend.entity.User;
import com.slate.backend.exception.EmailAlreadyUsedException;
import com.slate.backend.exception.InvalidPasswordChangeException;
import com.slate.backend.exception.ResourceNotFoundException;
import com.slate.backend.exception.UnauthorizedException;
import com.slate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        return toProfileResponse(getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException("El email ya está utilizado");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public void changeCurrentUserPassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException(
                    "La contraseña actual es incorrecta");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new InvalidPasswordChangeException(
                    "La nueva contraseña debe ser diferente de la actual");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Usuario no autenticado");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }
}
