package com.slate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.backend.dto.UserProfileResponse;
import com.slate.backend.exception.GlobalExceptionHandler;
import com.slate.backend.service.AuthService;
import com.slate.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateProfile_returnsSafeResponseWithoutPassword() throws Exception {
        when(userService.updateCurrentUserProfile(any())).thenReturn(
                UserProfileResponse.builder()
                        .id(1L)
                        .name("Nuevo nombre")
                        .email("nuevo@email.com")
                        .phone("123")
                        .build());

        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("name", "Nuevo nombre", "email", "nuevo@email.com", "phone", "123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo nombre"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getProfile_returnsSafeResponseWithoutPassword() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(
                UserProfileResponse.builder()
                        .id(1L)
                        .name("Nombre actual")
                        .email("actual@email.com")
                        .phone("123")
                        .build());

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre actual"))
                .andExpect(jsonPath("$.email").value("actual@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updateProfile_rejectsBlankName() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"valid@email.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_rejectsInvalidEmail() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nombre\",\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"actual\",\"newPassword\":\"nueva123\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void changePassword_rejectsNewPasswordShorterThanSixCharacters() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"actual\",\"newPassword\":\"12345\"}"))
                .andExpect(status().isBadRequest());
    }
}
