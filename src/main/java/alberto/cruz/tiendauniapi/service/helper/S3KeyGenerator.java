package alberto.cruz.tiendauniapi.service.helper;

import alberto.cruz.tiendauniapi.service.model.PresignedUrlMimeExtension;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class S3KeyGenerator {

    private static final String PROFILE_FOLDER = "profiles";
    private static final String PUBLICATION_FOLDER = "publications";
    private static final String KEY_SEPARATOR = "/";
    private static final String EXTENSION_SEPARATOR = ".";

    public String generateProfileKey(UUID userId, String mimeType) {
        return buildKey(PROFILE_FOLDER, userId, mimeType);
    }

    public String generatePublicationKey(UUID userId, String mimeType) {
        return buildKey(PUBLICATION_FOLDER, userId, mimeType);
    }

    private String buildKey(String folder, UUID userId, String mimeType) {
        String extension = resolveExtension(mimeType);
        return folder
                + KEY_SEPARATOR
                + userId
                + KEY_SEPARATOR
                + UUID.randomUUID()
                + EXTENSION_SEPARATOR
                + extension;
    }

    private String resolveExtension(String mimeType) {
        return PresignedUrlMimeExtension.fromMimeType(mimeType)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported mime type: " + mimeType))
                .extension();
    }
}