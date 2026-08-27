package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.service.exception.UserNotFoundException;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileAvatarController.class)
@Import(ProfileAvatarControllerWebMvcTest.TestSecurityConfig.class)
class ProfileAvatarControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void validKey_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        UUID fileId = UUID.randomUUID();
        String key = "profiles/" + userId + "/" + fileId + ".jpg";

        doNothing().when(authenticationService).updateAvatarKey(eq(userId), eq(key));

        String body = "{\"key\":\"" + key + "\"}";

        mockMvc.perform(patch("/profiles/me/avatar")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidKeyFormat_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        String body = "{\"key\":\"../escape\"}";

        mockMvc.perform(patch("/profiles/me/avatar")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(authenticationService);
    }

    @Test
    void userNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID universityId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser("alice@example.com", "secret", userId, universityId);

        UUID fileId = UUID.randomUUID();
        String key = "profiles/" + userId + "/" + fileId + ".jpg";

        doThrow(new UserNotFoundException())
                .when(authenticationService).updateAvatarKey(eq(userId), eq(key));

        String body = "{\"key\":\"" + key + "\"}";

        mockMvc.perform(patch("/profiles/me/avatar")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        String body = "{\"key\":\"profiles/00000000-0000-0000-0000-000000000000/00000000-0000-0000-0000-000000000000.jpg\"}";

        mockMvc.perform(patch("/profiles/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test-only security filter chain that mirrors the production authorization
     * rules without pulling in production beans that depend on the database or
     * externalized configuration. {@code httpBasic} is enabled so the default
     * {@code BasicAuthenticationEntryPoint} returns 401 for protected endpoints
     * accessed without credentials.
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
