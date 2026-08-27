package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicationPresignedUrlController.class)
@Import(PublicationPresignedUrlControllerWebMvcTest.TestSecurityConfig.class)
class PublicationPresignedUrlControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresignedUrlService presignedUrlService;

    @Test
    void validFiles_returns200WithPreservedOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        PresignedUrlItem first = new PresignedUrlItem("file1", "demo-front", 1024L, "image/jpeg");
        PresignedUrlItem second = new PresignedUrlItem(
                "7194b889-868c-47c8-8431-4cb4464a15a4", "demo", 5_242_880L, "video/mp4");
        PresignedUrlItem third = new PresignedUrlItem("file3", "test_123", 2048L, "image/png");

        List<PresignedUrl> serviceResult = List.of(
                new PresignedUrl("https://example.com/publications/uuid-1.jpg?X-Amz-Sig=1",
                        "publications/" + userId + "/uuid-1.jpg"),
                new PresignedUrl("https://example.com/publications/uuid-2.mp4?X-Amz-Sig=2",
                        "publications/" + userId + "/uuid-2.mp4"),
                new PresignedUrl("https://example.com/publications/uuid-3.png?X-Amz-Sig=3",
                        "publications/" + userId + "/uuid-3.png")
        );

        when(presignedUrlService.generatePublicationPresignedUrls(
                eq(userId), any(PresignedUrlPublicationRequest.class), eq(BucketTarget.PUBLICATION)))
                .thenReturn(serviceResult);

        String body = objectMapper.writeValueAsString(
                new PresignedUrlPublicationRequest(new ArrayList<>(List.of(first, second, third))));

        mockMvc.perform(post("/posts/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uris.length()").value(3))
                .andExpect(jsonPath("$.uris[0].id").value("file1"))
                .andExpect(jsonPath("$.uris[0].url").value(containsString("uuid-1.jpg")))
                .andExpect(jsonPath("$.uris[0].key").value(containsString("uuid-1.jpg")))
                .andExpect(jsonPath("$.uris[1].id").value("7194b889-868c-47c8-8431-4cb4464a15a4"))
                .andExpect(jsonPath("$.uris[1].url").value(containsString("uuid-2.mp4")))
                .andExpect(jsonPath("$.uris[2].id").value("file3"))
                .andExpect(jsonPath("$.uris[2].url").value(containsString("uuid-3.png")));
    }

    @Test
    void duplicateIds_returns400WithMentionOfConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        PresignedUrlItem first = new PresignedUrlItem("file1", "front", 1024L, "image/jpeg");
        PresignedUrlItem second = new PresignedUrlItem("file1", "back", 1024L, "image/jpeg");

        String body = objectMapper.writeValueAsString(
                new PresignedUrlPublicationRequest(new ArrayList<>(List.of(first, second))));

        mockMvc.perform(post("/posts/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("file1")));

        verifyNoInteractions(presignedUrlService);
    }

    @Test
    void emptyArray_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        String body = "{\"files\":[]}";

        mockMvc.perform(post("/posts/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(presignedUrlService);
    }

    @Test
    void over10Files_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        List<PresignedUrlItem> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(new PresignedUrlItem("id-" + i, "name" + i, 1024L, "image/jpeg"));
        }
        String body = objectMapper.writeValueAsString(new PresignedUrlPublicationRequest(files));

        mockMvc.perform(post("/posts/presigned-url")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(presignedUrlService);
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        PresignedUrlItem first = new PresignedUrlItem("file1", "front", 1024L, "image/jpeg");
        String body = objectMapper.writeValueAsString(
                new PresignedUrlPublicationRequest(new ArrayList<>(List.of(first))));

        mockMvc.perform(post("/posts/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(presignedUrlService);
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