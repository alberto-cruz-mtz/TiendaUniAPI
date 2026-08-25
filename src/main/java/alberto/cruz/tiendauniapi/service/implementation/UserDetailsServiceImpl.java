package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        UserProjection userEntity = userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró ninguna cuenta con la dirección de correo electrónico: " + username));

        return this.createUserDetails(userEntity);
    }

    private AuthenticatedUser createUserDetails(UserProjection user) {
        return new AuthenticatedUser(
                user.getEmail(),
                user.getPassword(),
                user.getId(),
                user.getUniversityId()
        );
    }
}