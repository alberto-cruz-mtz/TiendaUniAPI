package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfilePresignedUrlController.class)
@Import(ProfilePresignedUrlControllerWebMvcTest.TestSecurityConfig.class)
class ProfilePresignedUrlControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresignedUrlService presignedUrlService;

    @Test
    void validRequest_returns200WithUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        UUID fileId = UUID.randomUUID();
        String url = "http://localhost:4566/test-profile/profiles/" + userId + "/" + fileId + ".jpg";
        String key = "profiles/" + userId + "/" + fileId + ".jpg";
        when(presignedUrlService.generateProfilePresignedUrl(eq(userId), any(PresignedUrlProfileRequest.class), eq(BucketTarget.PROFILE)))
                .thenReturn(new PresignedUrl(url, key));

        String body = objectMapper.writeValueAsString(new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg"));

        mockMvc.perform(post("/profiles/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("test-profile")));
    }

    @Test
    void invalidMimeType_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        String body = objectMapper.writeValueAsString(new PresignedUrlProfileRequest("avatar", 1024L, "text/plain"));

        mockMvc.perform(post("/profiles/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overSize_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        long overSize = 11_000_000L; // 11MB > 10MB image cap
        String body = objectMapper.writeValueAsString(new PresignedUrlProfileRequest("avatar", overSize, "image/jpeg"));

        mockMvc.perform(post("/profiles/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg"));

        mockMvc.perform(post("/profiles/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test-only security filter chain that mirrors the production authorization
     * rules without pulling in production beans that depend on the database
     * (UserDetailsService) or externalized configuration
     * ({@code app.cors.allowed-origins}).
     */
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(httpAuth -> {
                        httpAuth.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/signup", "/auth/refresh").permitAll();
                        httpAuth.anyRequest().authenticated();
                    })
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
