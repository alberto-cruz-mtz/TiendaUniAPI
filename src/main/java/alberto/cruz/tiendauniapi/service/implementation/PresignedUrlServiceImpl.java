package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.configuration.AwsS3Properties;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.service.exception.PresignedUrlGenerationException;
import alberto.cruz.tiendauniapi.service.helper.S3KeyGenerator;
import alberto.cruz.tiendauniapi.service.interfaces.PresignedUrlService;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Generates presigned S3 PUT URLs using bucket-scoped presigner beans.
 *
 * <p>The two {@code S3Presigner} beans ({@code profileS3Presigner} and
 * {@code publicationS3Presigner}) carry their own {@code endpointOverride}, which is
 * why we resolve the presigner locally instead of threading a single bean through a
 * map. This keeps the bucket configuration per bucket and lets the deployment swap
 * providers (AWS → Cloudflare R2) by changing env vars without recompiling.
 */
@Service
@RequiredArgsConstructor
public class PresignedUrlServiceImpl implements PresignedUrlService {

    private static final Duration SIGNATURE_DURATION = Duration.ofMinutes(5);
    private static final String PRESIGNED_URL_FAILURE_MESSAGE = "No se pudo generar la URL pre-firmada.";

    @Qualifier("profileS3Presigner")
    private final S3Presigner profileS3Presigner;

    @Qualifier("publicationS3Presigner")
    private final S3Presigner publicationS3Presigner;

    private final S3KeyGenerator s3KeyGenerator;
    private final AwsS3Properties awsS3Properties;

    @Override
    @Transactional(readOnly = true)
    public PresignedUrl generateProfilePresignedUrl(UUID userId, PresignedUrlProfileRequest request, BucketTarget target) {
        String key = s3KeyGenerator.generateProfileKey(userId, request.mimeType());
        return sign(target, key, request.mimeType(), request.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PresignedUrl> generatePublicationPresignedUrls(UUID userId, PresignedUrlPublicationRequest request, BucketTarget target) {
        return request.files().stream()
                .map(file -> sign(
                        target,
                        s3KeyGenerator.generatePublicationKey(userId, file.mimeType()),
                        file.mimeType(),
                        file.size()
                ))
                .toList();
    }

    private PresignedUrl sign(BucketTarget target, String key, String mimeType, Long size) {
        S3Presigner presigner = resolvePresigner(target);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(target.resolveBucketName(awsS3Properties))
                .key(key)
                .contentType(mimeType)
                .contentLength(size)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(SIGNATURE_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedResult;
        try {
            presignedResult = presigner.presignPutObject(presignRequest);
        } catch (RuntimeException sdkFailure) {
            throw new PresignedUrlGenerationException(PRESIGNED_URL_FAILURE_MESSAGE, sdkFailure);
        }

        return new PresignedUrl(presignedResult.url().toString(), key);
    }

    private S3Presigner resolvePresigner(BucketTarget target) {
        return switch (target) {
            case PROFILE -> profileS3Presigner;
            case PUBLICATION -> publicationS3Presigner;
        };
    }
}