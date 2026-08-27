package alberto.cruz.tiendauniapi.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class AwsS3Configuration {

    @Bean
    public S3Presigner profileS3Presigner(AwsS3Properties properties) {
        return buildPresigner(properties, properties.bucketProfileUrl());
    }

    @Bean
    public S3Presigner publicationS3Presigner(AwsS3Properties properties) {
        return buildPresigner(properties, properties.bucketPublicationUrl());
    }

    private static S3Presigner buildPresigner(AwsS3Properties properties, String bucketUrl) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider);

        if (bucketUrl != null && !bucketUrl.isBlank()) {
            builder.endpointOverride(URI.create(bucketUrl));
        }

        return builder.build();
    }
}