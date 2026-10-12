package com.slate.backend.service;

import com.slate.backend.dto.AuthResponse;
import com.slate.backend.dto.LoginRequest;
import com.slate.backend.dto.RegisterRequest;
import com.slate.backend.entity.User;
import com.slate.backend.repository.UserRepository;
import com.slate.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ---------------------------------------------------------------
    // Register
    // ---------------------------------------------------------------

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .name(saved.getName())
                .id(saved.getId())
                .build();
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .id(user.getId())
                .build();
    }
}
