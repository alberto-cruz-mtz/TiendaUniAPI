package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItemResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationResponse;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/posts/presigned-url")
@RequiredArgsConstructor
public class PublicationPresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @PostMapping
    public ResponseEntity<PresignedUrlPublicationResponse> generate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody PresignedUrlPublicationRequest request
    ) {
        List<PresignedUrl> presignedUrls = presignedUrlService.generatePublicationPresignedUrls(
                authenticatedUser.getUserId(), request, BucketTarget.PUBLICATION);

        List<PresignedUrlItem> inputFiles = request.files();
        List<PresignedUrlItemResponse> uris = new ArrayList<>(presignedUrls.size());
        for (int i = 0; i < presignedUrls.size(); i++) {
            PresignedUrlItem inputFile = inputFiles.get(i);
            PresignedUrl presignedUrl = presignedUrls.get(i);
            uris.add(new PresignedUrlItemResponse(inputFile.id(), presignedUrl.url(), presignedUrl.key()));
        }

        return ResponseEntity.ok(new PresignedUrlPublicationResponse(uris));
    }
}