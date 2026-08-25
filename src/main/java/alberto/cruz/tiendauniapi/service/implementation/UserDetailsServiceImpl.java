package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.utils.mapper.UserMapper;
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
        UserProjection userEntity = this.findUserByEmail(username);
        return UserMapper.toAuthenticatedUser(userEntity);
    }

    private UserProjection findUserByEmail(String email) {
        return userRepository.findUserEntitiesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró ninguna cuenta con la dirección de correo electrónico: " + email));
    }
}