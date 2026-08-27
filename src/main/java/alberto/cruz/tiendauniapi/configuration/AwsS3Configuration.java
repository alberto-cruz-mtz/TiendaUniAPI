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
        return buildPresigner(properties);
    }

    @Bean
    public S3Presigner publicationS3Presigner(AwsS3Properties properties) {
        return buildPresigner(properties);
    }

    private static S3Presigner buildPresigner(AwsS3Properties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        URI uri = URI.create(properties.endpoint());
        S3Presigner.Builder builder = S3Presigner.builder()
                .endpointOverride(uri)
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider);

        return builder.build();
    }
}