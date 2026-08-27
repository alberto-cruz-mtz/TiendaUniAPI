package alberto.cruz.tiendauniapi.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AwsS3ConfigurationTest {

        private static final String ENDPOINT = "http://s3.localhost.localstack.cloud:4566";
        private static final String PROFILE_BUCKET_URL = "http://localhost:4566/bucket-profile-dev";
        private static final String PUBLICATION_BUCKET_URL = "http://localhost:4566/bucket-publication-dev";
        private static final String REGION = "us-east-1";
        private static final String ACCESS_KEY = "test-access-key";
        private static final String SECRET_KEY = "test-secret-key";
        private static final String PROFILE_BUCKET_NAME = "bucket-profile";
        private static final String PUBLICATION_BUCKET_NAME = "bucket-publication";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AwsS3Configuration.class, TestConfiguration.class)
            .withPropertyValues(
                    "app.aws.s3.access-key-id=" + ACCESS_KEY,
                    "app.aws.s3.secret-access-key=" + SECRET_KEY,
                    "app.aws.s3.region=" + REGION,
                    "app.aws.s3.endpoint=" + ENDPOINT,
                    "app.aws.s3.bucket-profile-name=" + PROFILE_BUCKET_NAME,
                    "app.aws.s3.bucket-publication-name=" + PUBLICATION_BUCKET_NAME,
                    "app.aws.s3.bucket-profile-url=" + PROFILE_BUCKET_URL,
                    "app.aws.s3.bucket-publication-url=" + PUBLICATION_BUCKET_URL
            );

    @Test
    void contextLoads_whenAllPropertiesPresent() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AwsS3Properties.class);
            assertThat(context).hasBean("profileS3Presigner");
            assertThat(context).hasBean("publicationS3Presigner");

            S3Presigner profilePresigner = context.getBean("profileS3Presigner", S3Presigner.class);
            S3Presigner publicationPresigner = context.getBean("publicationS3Presigner", S3Presigner.class);

            String profileUrl = profilePresigner
                    .presignPutObject(samplePutPresignRequest(PROFILE_BUCKET_NAME, "profiles/sample/foo.jpg"))
                    .url()
                    .toString();
            String publicationUrl = publicationPresigner
                    .presignPutObject(samplePutPresignRequest(PUBLICATION_BUCKET_NAME, "publications/sample/foo.mp4"))
                    .url()
                    .toString();

            assertThat(java.net.URI.create(profileUrl).getHost()).contains(java.net.URI.create(ENDPOINT).getHost());
            assertThat(java.net.URI.create(publicationUrl).getHost()).contains(java.net.URI.create(ENDPOINT).getHost());
        });
    }

    @Test
    void contextFailsToStart_whenBucketProfileUrlIsBlank() {
        new ApplicationContextRunner()
                .withUserConfiguration(AwsS3Configuration.class, TestConfiguration.class)
                .withPropertyValues(
                        "app.aws.s3.access-key-id=" + ACCESS_KEY,
                        "app.aws.s3.secret-access-key=" + SECRET_KEY,
                        "app.aws.s3.region=" + REGION,
                        "app.aws.s3.endpoint=" + ENDPOINT,
                        "app.aws.s3.bucket-profile-name=" + PROFILE_BUCKET_NAME,
                        "app.aws.s3.bucket-publication-name=" + PUBLICATION_BUCKET_NAME,
                        "app.aws.s3.bucket-profile-url=",
                        "app.aws.s3.bucket-publication-url=" + PUBLICATION_BUCKET_URL
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void presignerBeans_areDistinctInstances() {
        runner.run(context -> {
            S3Presigner profilePresigner = context.getBean("profileS3Presigner", S3Presigner.class);
            S3Presigner publicationPresigner = context.getBean("publicationS3Presigner", S3Presigner.class);

            assertThat(profilePresigner).isNotSameAs(publicationPresigner);
        });
    }

    private static PutObjectPresignRequest samplePutPresignRequest(String bucketName, String key) {
        return PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/octet-stream")
                        .contentLength(1024L)
                        .build())
                .build();
    }

    @Configuration
    @EnableConfigurationProperties(AwsS3Properties.class)
    static class TestConfiguration {
    }
}