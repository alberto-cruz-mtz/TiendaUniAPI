package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.configuration.AwsS3Properties;

public enum BucketTarget {

    PROFILE,
    PUBLICATION;

    public String resolveBucketName(AwsS3Properties properties) {
        return switch (this) {
            case PROFILE -> properties.bucketProfileName();
            case PUBLICATION -> properties.bucketPublicationName();
        };
    }

    public String resolveBucketUrl(AwsS3Properties properties) {
        return switch (this) {
            case PROFILE -> properties.bucketProfileUrl();
            case PUBLICATION -> properties.bucketPublicationUrl();
        };
    }
}