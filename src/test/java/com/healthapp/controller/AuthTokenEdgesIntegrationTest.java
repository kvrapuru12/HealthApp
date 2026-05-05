package com.healthapp.controller;

import com.healthapp.service.AppleTokenVerifierService;
import com.healthapp.service.GoogleTokenVerifierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthTokenEdgesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @MockBean
    private AppleTokenVerifierService appleTokenVerifierService;

    @Test
    void refresh_returnsUnauthorizedForInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid-refresh\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void googleSignIn_returnsUnauthorizedWhenVerifierRejectsToken() throws Exception {
        when(googleTokenVerifierService.verifyToken("bad-id-token", "android")).thenReturn(null);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad-id-token\",\"platform\":\"android\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ID_TOKEN"));
    }

    @Test
    void appleSignIn_returnsUnauthorizedWhenVerifierRejectsToken() throws Exception {
        when(appleTokenVerifierService.verifyToken("bad-apple-token", "ios")).thenReturn(null);

        mockMvc.perform(post("/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad-apple-token\",\"platform\":\"ios\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ID_TOKEN"));
    }
}
