package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.service.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplUpdateAvatarKeyTest {

    private static final UUID USER_ID = UUID.fromString("4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c");
    private static final String NEW_KEY = "profiles/" + USER_ID + "/8a3e2c11-1111-2222-3333-444455556666.jpg";

    @Mock
    private UserRepository userRepository;

    @Mock
    private alberto.cruz.tiendauniapi.persistence.repository.UniversityRepository universityRepository;

    @Mock
    private alberto.cruz.tiendauniapi.service.interfaces.RefreshTokenService refreshTokenService;

    @Mock
    private alberto.cruz.tiendauniapi.utils.JwtUtil jwtUtil;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("updateAvatarKey_validKey_updatesEntityAndPersists")
    void updateAvatarKey_validKey_updatesEntityAndPersists() {
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .avatarUrl("profiles/old-user/old-uuid.jpg")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authenticationService.updateAvatarKey(USER_ID, NEW_KEY);

        verify(userRepository).findById(USER_ID);
        verify(userRepository).save(user);
        assertThat(user.getAvatarUrl()).isEqualTo(NEW_KEY);
    }

    @Test
    @DisplayName("updateAvatarKey_userNotFound_throwsUserNotFoundException")
    void updateAvatarKey_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.updateAvatarKey(USER_ID, NEW_KEY))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(USER_ID);
    }
}
