package com.healthapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.config.JwtAuthenticationFilter;
import com.healthapp.dto.IssuedAuthTokens;
import com.healthapp.dto.RefreshRotationResult;
import com.healthapp.entity.User;
import com.healthapp.service.AppleTokenVerifierService;
import com.healthapp.service.GoogleTokenVerifierService;
import com.healthapp.service.RefreshTokenService;
import com.healthapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;
    @MockBean
    private AppleTokenVerifierService appleTokenVerifierService;
    @MockBean
    private RefreshTokenService refreshTokenService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void login_returnsBadRequestWhenPasswordDoesNotMatch() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setPassword("storedHash");
        user.setAccountStatus(User.AccountStatus.ACTIVE);

        when(userService.getUserByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "storedHash")).thenReturn(false);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Authentication failed"));
    }

    @Test
    void login_returnsTokensForValidCredentials() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Doe");
        user.setPassword("storedHash");
        user.setRole(User.UserRole.USER);
        user.setGender(User.Gender.FEMALE);
        user.setDob(LocalDate.now().minusYears(25));
        user.setAccountStatus(User.AccountStatus.ACTIVE);

        when(userService.getUserByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Correct1!", "storedHash")).thenReturn(true);
        when(refreshTokenService.issueNewSession(user))
                .thenReturn(new IssuedAuthTokens("access-token", "refresh-token", 900));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"Correct1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refresh_returnsUnauthorizedWhenRotationFails() throws Exception {
        when(refreshTokenService.rotateRefreshToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.healthapp.dto.RefreshTokenRequest() {{
                            setRefreshToken("bad-token");
                        }})))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_returnsNewTokensWhenRotationSucceeds() throws Exception {
        User user = new User();
        user.setId(9L);
        user.setUsername("bob");
        user.setRole(User.UserRole.USER);
        user.setAccountStatus(User.AccountStatus.ACTIVE);

        RefreshRotationResult rotationResult = new RefreshRotationResult(
                user,
                new IssuedAuthTokens("new-access", "new-refresh", 1200)
        );
        when(refreshTokenService.rotateRefreshToken("ok-token")).thenReturn(Optional.of(rotationResult));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"ok-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }
}
