package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
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

    private final UserService userService;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        UserProjection userEntity = userService.getUserProjectionByEmail(username);
        return UserMapper.toAuthenticatedUser(userEntity);
    }
}