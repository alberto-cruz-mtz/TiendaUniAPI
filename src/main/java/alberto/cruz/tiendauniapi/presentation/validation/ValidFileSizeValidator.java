package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Locale;

/**
 * Class-level validator that enforces the size caps defined by {@link ValidFileSize}.
 *
 * <p>Dispatch is driven by {@code instanceof} so the same annotation can be applied
 * to single-file request DTOs ({@link PresignedUrlProfileRequest},
 * {@link PresignedUrlItem}) and to multi-file request DTOs
 * ({@link PresignedUrlPublicationRequest}). Any other target type is treated as a
 * programming error and surfaces as an {@link IllegalStateException} so the misuse
 * fails loudly during development.
 *
 * <p>Caps: {@code image/*} (including {@code image/svg+xml}) up to 10 MB,
 * {@code video/*} up to 50 MB. Non-positive sizes are delegated to {@code @Positive}
 * by returning {@code true}.
 */
public class ValidFileSizeValidator implements ConstraintValidator<ValidFileSize, Object> {

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_VIDEO_BYTES = 50L * 1024L * 1024L;
    private static final String IMAGE_PREFIX = "image/";
    private static final String VIDEO_PREFIX = "video/";

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value instanceof PresignedUrlProfileRequest profileRequest) {
            return isWithinCap(profileRequest.mimeType(), profileRequest.size());
        }
        if (value instanceof PresignedUrlItem item) {
            return isWithinCap(item.mimeType(), item.size());
        }
        if (value instanceof PresignedUrlPublicationRequest publicationRequest) {
            List<PresignedUrlItem> files = publicationRequest.files();
            if (files == null) {
                return true;
            }
            for (PresignedUrlItem file : files) {
                if (!isWithinCap(file.mimeType(), file.size())) {
                    return false;
                }
            }
            return true;
        }
        throw new IllegalStateException(
                "Unsupported type for @ValidFileSize: " + value.getClass().getName()
        );
    }

    private boolean isWithinCap(String mimeType, Long size) {
        if (size == null || size <= 0) {
            return true;
        }
        return size <= capForMimeType(mimeType);
    }

    private static long capForMimeType(String mimeType) {
        if (mimeType == null) {
            return MAX_IMAGE_BYTES;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith(VIDEO_PREFIX)) {
            return MAX_VIDEO_BYTES;
        }
        return MAX_IMAGE_BYTES;
    }
}
