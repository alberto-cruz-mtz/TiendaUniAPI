package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;

import java.util.List;
import java.util.UUID;

/**
 * Generates presigned S3 PUT URLs for avatar and publication uploads.
 *
 * <p>The service is bucket-agnostic: the caller passes a {@link BucketTarget} that
 * selects the appropriate {@code S3Presigner} bean and resolves bucket names. The
 * {@code userId} is required because every key is folderized as
 * {@code profiles/<userId>/<uuid>.<ext>} or {@code publications/<userId>/<uuid>.<ext>}
 * to scope objects per user and avoid collisions.
 */
public interface PresignedUrlService {

    PresignedUrl generateProfilePresignedUrl(UUID userId, PresignedUrlProfileRequest request, BucketTarget target);

    List<PresignedUrl> generatePublicationPresignedUrls(UUID userId, PresignedUrlPublicationRequest request, BucketTarget target);
}