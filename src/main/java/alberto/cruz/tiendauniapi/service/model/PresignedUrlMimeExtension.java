package alberto.cruz.tiendauniapi.service.model;

import java.util.Optional;

public enum PresignedUrlMimeExtension {

    JPEG("image/jpeg", "jpg"),
    JPG("image/jpg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp"),
    SVG_XML("image/svg+xml", "svg"),
    MP4("video/mp4", "mp4"),
    WEBM("video/webm", "webm"),
    OGG("video/ogg", "ogg");

    private final String mimeType;
    private final String extension;

    PresignedUrlMimeExtension(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<PresignedUrlMimeExtension> fromMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }

        for (PresignedUrlMimeExtension candidate : values()) {
            if (candidate.mimeType.equalsIgnoreCase(mimeType)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    public static java.util.Set<String> whitelist() {
        java.util.Set<String> mimeTypes = new java.util.LinkedHashSet<>();
        for (PresignedUrlMimeExtension entry : values()) {
            mimeTypes.add(entry.mimeType);
        }
        return java.util.Collections.unmodifiableSet(mimeTypes);
    }
}