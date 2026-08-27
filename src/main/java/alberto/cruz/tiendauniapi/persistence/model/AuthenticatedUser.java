package alberto.cruz.tiendauniapi.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public final class AuthenticatedUser implements UserDetails {

    private final String username;
    private final String password;
    private final UUID userId;
    private final UUID universityId;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
}