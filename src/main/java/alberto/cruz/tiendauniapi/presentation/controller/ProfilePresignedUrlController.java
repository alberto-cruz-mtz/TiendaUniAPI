package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileResponse;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles/presigned-url")
@RequiredArgsConstructor
public class ProfilePresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @PostMapping
    public ResponseEntity<PresignedUrlProfileResponse> generate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody PresignedUrlProfileRequest request
    ) {
        PresignedUrl presignedUrl = presignedUrlService.generateProfilePresignedUrl(
                authenticatedUser.getUserId(), request, BucketTarget.PROFILE);
        return ResponseEntity.ok(new PresignedUrlProfileResponse(presignedUrl.url()));
    }
}
