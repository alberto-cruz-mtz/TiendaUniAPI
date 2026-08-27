package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.configuration.AwsS3Properties;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.service.helper.S3KeyGenerator;
import alberto.cruz.tiendauniapi.service.model.BucketTarget;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Regression test for AC-OBS-1: the pre-signed URL service MUST NOT log signed
 * URLs (or anything containing the SigV4 query token) on the happy path. The
 * service is permitted to log warnings/errors only on failure; any future change
 * that adds a "successful" log line must avoid emitting the signed URL.
 *
 * <p>The test attaches a Logback {@link ListAppender} to the service's logger
 * (the class itself — the implementation does not currently declare one, so the
 * appender catches root events too via additivity).
 */
@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceImplLoggingTest {

    private static final UUID USER_ID = UUID.fromString("4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c");
    private static final String PROFILE_BUCKET_NAME = "test-bucket-profile";
    private static final String PROFILE_BUCKET_URL = "http://localhost:4566/test-profile";

    private static final URL SIGNED_URL;

    static {
        try {
            // A real signed URL carries the X-Amz-Signature query parameter; we
            // mimic that here so the assertion has something meaningful to search
            // for. If any log statement were to include this string, it would
            // constitute a leak.
            SIGNED_URL = new URL(
                    PROFILE_BUCKET_URL
                            + "/profiles/" + USER_ID + "/uuid.jpg"
                            + "?X-Amz-Signature=signed-signature-token"
                            + "&X-Amz-Date=20260101T000000Z"
                            + "&X-Amz-Credential=test%2F20260101%2Fus-east-1%2Fs3%2Faws4_request"
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

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void attachAppender() {
        service = new PresignedUrlServiceImpl(
                profileS3Presigner,
                publicationS3Presigner,
                s3KeyGenerator,
                awsS3Properties
        );

        when(awsS3Properties.bucketProfileName()).thenReturn(PROFILE_BUCKET_NAME);
        lenient().when(awsS3Properties.bucketProfileUrl()).thenReturn(PROFILE_BUCKET_URL);

        serviceLogger = (Logger) LoggerFactory.getLogger(PresignedUrlServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        serviceLogger.addAppender(listAppender);
    }

    @AfterEach
    void detachAppender() {
        if (serviceLogger != null && listAppender != null) {
            serviceLogger.detachAppender(listAppender);
        }
        listAppender.stop();
    }

    @Test
    @DisplayName("generateProfilePresignedUrl_happyPath_doesNotLogSignedUrl")
    void generateProfilePresignedUrl_happyPath_doesNotLogSignedUrl() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg");
        String generatedKey = "profiles/" + USER_ID + "/generated-uuid.jpg";

        when(s3KeyGenerator.generateProfileKey(USER_ID, "image/jpeg")).thenReturn(generatedKey);
        when(profileS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(buildPresignedResult(SIGNED_URL));

        // Trigger a level lower than INFO to catch every event, including DEBUG/TRACE
        // if any are added in the future.
        Level previousLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.TRACE);

        try {
            service.generateProfilePresignedUrl(USER_ID, request, BucketTarget.PROFILE);
        } finally {
            serviceLogger.setLevel(previousLevel);
        }

        // Strongest possible assertion: no log line at all was emitted. The service
        // design says logging on the happy path is unnecessary.
        assertThat(listAppender.list)
                .as("happy-path presigning MUST NOT emit any log event")
                .isEmpty();

        // Defensive secondary assertions: even if a future regression added a log
        // line, it MUST NOT contain URL-shaped strings or the SigV4 signature token.
        assertThat(listAppender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("https://"));
        assertThat(listAppender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("http://"));
        assertThat(listAppender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("X-Amz-Signature"));
        assertThat(listAppender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("signed-signature-token"));
    }

    @Test
    @DisplayName("generateProfilePresignedUrl_happyPath_noAppenderEventContainsTheSignedUrl")
    void generateProfilePresignedUrl_happyPath_noAppenderEventContainsTheSignedUrl() {
        // Mirrors the test above but uses the permissive assertion (a log event
        // MAY exist for observability, but MUST NOT contain URL material). This
        // protects the contract even if the team adds structured logging later.
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg");
        String generatedKey = "profiles/" + USER_ID + "/generated-uuid.jpg";

        when(s3KeyGenerator.generateProfileKey(USER_ID, "image/jpeg")).thenReturn(generatedKey);
        when(profileS3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(buildPresignedResult(SIGNED_URL));

        Level previousLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.TRACE);

        try {
            service.generateProfilePresignedUrl(USER_ID, request, BucketTarget.PROFILE);
        } finally {
            serviceLogger.setLevel(previousLevel);
        }

        for (ILoggingEvent event : listAppender.list) {
            String formatted = event.getFormattedMessage();
            assertThat(formatted)
                    .as("logged message must not contain the URL or SigV4 token")
                    .doesNotContain("https://")
                    .doesNotContain("http://")
                    .doesNotContain("X-Amz-Signature");
        }
    }

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
