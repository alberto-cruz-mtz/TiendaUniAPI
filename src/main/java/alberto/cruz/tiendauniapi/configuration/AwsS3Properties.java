package alberto.cruz.tiendauniapi.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.aws.s3")
public record AwsS3Properties(
        @NotBlank String accessKeyId,
        @NotBlank String secretAccessKey,
        @NotBlank String region,
        @NotBlank String bucketProfileName,
        @NotBlank String bucketPublicationName,
        @NotBlank String bucketProfileUrl,
        @NotBlank String bucketPublicationUrl
) {
}