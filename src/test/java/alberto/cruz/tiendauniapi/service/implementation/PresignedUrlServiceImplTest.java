package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.configuration.AwsS3Properties;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import alberto.cruz.tiendauniapi.service.exception.PresignedUrlGenerationException;
import alberto.cruz.tiendauniapi.service.helper.S3KeyGenerator;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import alberto.cruz.tiendauniapi.service.model.PresignedUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c");
    private static final String PROFILE_BUCKET_NAME = "test-bucket-profile";
    private static final String PUBLICATION_BUCKET_NAME = "test-bucket-publication";
    private static final String PROFILE_BUCKET_URL = "http://localhost:4566/test-profile";
    private static final String PUBLICATION_BUCKET_URL = "http://localhost:4566/test-publication";

    /**
     * The SDK's {@code PresignedRequest.url()} derives the URL from {@code httpRequest.getUri()}, which contains
     * host + path but NOT the SigV4 query parameters added by the real presigner. So the URLs returned by the
     * mocked presigner below intentionally match what {@code url()} returns when we build the fixture the same way
     * — host + path only, no query string.
     */
    private static final URL PROFILE_PRESIGNED_URL;
    private static final URL PUBLICATION_FIRST_PRESIGNED_URL;
    private static final URL PUBLICATION_SECOND_PRESIGNED_URL;
    private static final URL PUBLICATION_THIRD_PRESIGNED_URL;

    static {
        try {
            PROFILE_PRESIGNED_URL = new URL(
                    PROFILE_BUCKET_URL
                            + "/profiles/" + USER_ID + "/avatar-uuid.jpg"
            );
            PUBLICATION_FIRST_PRESIGNED_URL = new URL(
                    PUBLICATION_BUCKET_URL
                            + "/publications/" + USER_ID + "/uuid-1.jpg"
            );
            PUBLICATION_SECOND_PRESIGNED_URL = new URL(
                    PUBLICATION_BUCKET_URL
                            + "/publications/" + USER_ID + "/uuid-2.mp4"
            );
            PUBLICATION_THIRD_PRESIGNED_URL = new URL(
                    PUBLICATION_BUCKET_URL
                            + "/publications/" + USER_ID + "/uuid-3.png"
            );
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Test fixture has invalid URL", ex);
        }
    }

    @Mock
    private S3Presigner profileS3Presigner;

    @Mock
    private S3Presigner publicationS3Presigner;

    @Mock
    private S3KeyGenerator s3KeyGenerator;

    @Mock
    private AwsS3Properties awsS3Properties;

    private PresignedUrlServiceImpl service;

    @BeforeEach
    void setUpService() {
        service = new PresignedUrlServiceImpl(
                profileS3Presigner,
                publicationS3Presigner,
                s3KeyGenerator,
                awsS3Properties
        );

        lenient().when(awsS3Properties.bucketProfileName()).thenReturn(PROFILE_BUCKET_NAME);
        lenient().when(awsS3Properties.bucketPublicationName()).thenReturn(PUBLICATION_BUCKET_NAME);
        lenient().when(awsS3Properties.bucketProfileUrl()).thenReturn(PROFILE_BUCKET_URL);
        lenient().when(awsS3Properties.bucketPublicationUrl()).thenReturn(PUBLICATION_BUCKET_URL);
    }

    @Test
    @DisplayName("generateProfilePresignedUrl_validRequest_returnsPresignedUrlWithCorrectKey")
    void generateProfilePresignedUrl_validRequest_returnsPresignedUrlWithCorrectKey() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg");
        String generatedKey = "profiles/" + USER_ID + "/generated-uuid.jpg";

        when(s3KeyGenerator.generateProfileKey(USER_ID, "image/jpeg")).thenReturn(generatedKey);
        when(profileS3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(buildPresignedResult(PROFILE_PRESIGNED_URL));

        PresignedUrl result = service.generateProfilePresignedUrl(USER_ID, request, BucketTarget.PROFILE);

        assertThat(result.url()).isEqualTo(PROFILE_PRESIGNED_URL.toString());
        assertThat(result.key()).isEqualTo(generatedKey);
        assertThat(result.key()).startsWith("profiles/");
        assertThat(result.key()).contains(USER_ID.toString());

        verify(profileS3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("generateProfilePresignedUrl_buildsPutObjectRequestWithBucketKeyContentTypeAndLength")
    void generateProfilePresignedUrl_buildsPutObjectRequestWithBucketKeyContentTypeAndLength() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("avatar", 2048L, "image/png");
        String generatedKey = "profiles/" + USER_ID + "/generated-uuid.png";

        when(s3KeyGenerator.generateProfileKey(USER_ID, "image/png")).thenReturn(generatedKey);
        when(profileS3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(buildPresignedResult(PROFILE_PRESIGNED_URL));

        service.generateProfilePresignedUrl(USER_ID, request, BucketTarget.PROFILE);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(profileS3Presigner).presignPutObject(captor.capture());

        PutObjectRequest putObjectRequest = captor.getValue().putObjectRequest();
        assertThat(putObjectRequest.bucket()).isEqualTo(PROFILE_BUCKET_NAME);
        assertThat(putObjectRequest.key()).isEqualTo(generatedKey);
        assertThat(putObjectRequest.contentType()).isEqualTo("image/png");
        assertThat(putObjectRequest.contentLength()).isEqualTo(2048L);
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("generateProfilePresignedUrl_s3PresignerThrows_wrapsInPresignedUrlGenerationException")
    void generateProfilePresignedUrl_s3PresignerThrows_wrapsInPresignedUrlGenerationException() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg");
        String generatedKey = "profiles/" + USER_ID + "/generated-uuid.jpg";
        S3Exception sdkFailure = (S3Exception) S3Exception.builder()
                .message("AWS caído")
                .statusCode(500)
                .build();

        when(s3KeyGenerator.generateProfileKey(USER_ID, "image/jpeg")).thenReturn(generatedKey);
        when(profileS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenThrow(sdkFailure);

        assertThatThrownBy(() -> service.generateProfilePresignedUrl(USER_ID, request, BucketTarget.PROFILE))
                .isInstanceOf(PresignedUrlGenerationException.class)
                .hasMessage("No se pudo generar la URL pre-firmada.")
                .hasCause(sdkFailure);
    }

    @Test
    @DisplayName("generatePublicationPresignedUrls_preservesOrderAndIds")
    void generatePublicationPresignedUrls_preservesOrderAndIds() {
        PresignedUrlItem first = new PresignedUrlItem("file1", "front", 1024L, "image/jpeg");
        PresignedUrlItem second = new PresignedUrlItem(
                "7194b889-868c-47c8-8431-4cb4464a15a4",
                "demo",
                5_242_880L,
                "video/mp4"
        );
        PresignedUrlItem third = new PresignedUrlItem("file3", "side", 2048L, "image/png");
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(first, second, third));

        String firstKey = "publications/" + USER_ID + "/uuid-1.jpg";
        String secondKey = "publications/" + USER_ID + "/uuid-2.mp4";
        String thirdKey = "publications/" + USER_ID + "/uuid-3.png";

        when(s3KeyGenerator.generatePublicationKey(USER_ID, "image/jpeg")).thenReturn(firstKey);
        when(s3KeyGenerator.generatePublicationKey(USER_ID, "video/mp4")).thenReturn(secondKey);
        when(s3KeyGenerator.generatePublicationKey(USER_ID, "image/png")).thenReturn(thirdKey);

        when(publicationS3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.<PutObjectPresignRequest>getArgument(0).putObjectRequest().key();
                    URL url;
                    if (firstKey.equals(key)) {
                        url = PUBLICATION_FIRST_PRESIGNED_URL;
                    } else if (secondKey.equals(key)) {
                        url = PUBLICATION_SECOND_PRESIGNED_URL;
                    } else if (thirdKey.equals(key)) {
                        url = PUBLICATION_THIRD_PRESIGNED_URL;
                    } else {
                        throw new IllegalStateException("Unexpected key in presignPutObject: " + key);
                    }
                    return buildPresignedResult(url);
                });

        List<PresignedUrl> result = service.generatePublicationPresignedUrls(USER_ID, request, BucketTarget.PUBLICATION);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).key()).isEqualTo(firstKey);
        assertThat(result.get(0).url()).isEqualTo(PUBLICATION_FIRST_PRESIGNED_URL.toString());
        assertThat(result.get(1).key()).isEqualTo(secondKey);
        assertThat(result.get(1).url()).isEqualTo(PUBLICATION_SECOND_PRESIGNED_URL.toString());
        assertThat(result.get(2).key()).isEqualTo(thirdKey);
        assertThat(result.get(2).url()).isEqualTo(PUBLICATION_THIRD_PRESIGNED_URL.toString());

        assertThat(result).allSatisfy(presignedUrl -> {
            assertThat(presignedUrl.key()).startsWith("publications/");
            assertThat(presignedUrl.key()).contains(USER_ID.toString());
            assertThat(presignedUrl.url()).startsWith(PUBLICATION_BUCKET_URL + "/publications/");
        });

        verify(publicationS3Presigner, times(3)).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("generatePublicationPresignedUrls_singleItem_generatesOnePresignedUrl")
    void generatePublicationPresignedUrls_singleItem_generatesOnePresignedUrl() {
        PresignedUrlItem only = new PresignedUrlItem("only", "solo", 1024L, "image/webp");
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(only));
        String generatedKey = "publications/" + USER_ID + "/uuid.webp";

        when(s3KeyGenerator.generatePublicationKey(USER_ID, "image/webp")).thenReturn(generatedKey);
        when(publicationS3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(buildPresignedResult(PUBLICATION_FIRST_PRESIGNED_URL));

        List<PresignedUrl> result = service.generatePublicationPresignedUrls(USER_ID, request, BucketTarget.PUBLICATION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo(generatedKey);
        assertThat(result.get(0).url()).isEqualTo(PUBLICATION_FIRST_PRESIGNED_URL.toString());
    }

    @Test
    @DisplayName("generatePublicationPresignedUrls_s3PresignerThrows_wrapsInPresignedUrlGenerationException")
    void generatePublicationPresignedUrls_s3PresignerThrows_wrapsInPresignedUrlGenerationException() {
        PresignedUrlItem only = new PresignedUrlItem("only", "solo", 1024L, "image/jpeg");
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(only));
        String generatedKey = "publications/" + USER_ID + "/uuid.jpg";
        S3Exception sdkFailure = (S3Exception) S3Exception.builder()
                .message("bucket down")
                .statusCode(500)
                .build();

        when(s3KeyGenerator.generatePublicationKey(USER_ID, "image/jpeg")).thenReturn(generatedKey);
        when(publicationS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenThrow(sdkFailure);

        assertThatThrownBy(() -> service.generatePublicationPresignedUrls(USER_ID, request, BucketTarget.PUBLICATION))
                .isInstanceOf(PresignedUrlGenerationException.class)
                .hasMessage("No se pudo generar la URL pre-firmada.")
                .hasCause(sdkFailure);
    }

    /**
     * Builds a real {@link PresignedPutObjectRequest} via the SDK builder. Mockito cannot reliably stub
     * {@code PresignedPutObjectRequest} because its abstract base carries private final fields that the
     * inline mock-maker cannot populate without calling it (which would require the protected constructor).
     */
    private static PresignedPutObjectRequest buildPresignedResult(URL url) {
        return PresignedPutObjectRequest.builder()
                .expiration(Instant.now().plus(Duration.ofMinutes(5)))
                .isBrowserExecutable(true)
                .signedHeaders(Map.of("host", List.of(url.getHost())))
                .httpRequest(SdkHttpRequest.builder()
                        .method(SdkHttpMethod.PUT)
                        .protocol(url.getProtocol())
                        .host(url.getHost())
                        .port(url.getPort() == -1 ? 443 : url.getPort())
                        .encodedPath(url.getPath())
                        .build())
                .build();
    }
}