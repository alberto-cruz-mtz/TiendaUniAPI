package alberto.cruz.tiendauniapi.configuration.security.filters;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.service.exception.InvalidTokenException;
import alberto.cruz.tiendauniapi.utils.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtUtil = jwtUtil;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Cookie cookie = WebUtils.getCookie(request, "access-token");

        if (cookie != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String accessToken = cookie.getValue();

            try {
                DecodedJWT decodedJWT = jwtUtil.validateToken(accessToken);

                String username = decodedJWT.getSubject();
                String userId = jwtUtil.getUserIdFromToken(decodedJWT);
                String tenantId = jwtUtil.getTenantIdFromToken(decodedJWT);

                UUID id = UUID.fromString(userId);
                UUID universityId = UUID.fromString(tenantId);

                AuthenticatedUser authenticatedUser = new AuthenticatedUser(username, null, id, universityId);
                Authentication authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException exception) {
                resolver.resolveException(request, response, null, exception);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
