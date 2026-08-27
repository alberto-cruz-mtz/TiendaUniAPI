package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.UpdateAvatarKeyRequest;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles/me/avatar")
@RequiredArgsConstructor
public class ProfileAvatarController {

    private final AuthenticationService authenticationService;

    @PatchMapping
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateAvatarKeyRequest request
    ) {
        authenticationService.updateAvatarKey(authenticatedUser.getUserId(), request.key());
        return ResponseEntity.noContent().build();
    }
}
