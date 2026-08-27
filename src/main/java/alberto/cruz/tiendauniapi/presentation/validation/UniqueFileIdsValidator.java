package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class-level validator that enforces id uniqueness across the {@code files} list of a
 * {@link PresignedUrlPublicationRequest}.
 *
 * <p>The check compares the size of a {@link Set} built from the ids against the
 * number of items. A mismatch means at least one id was added more than once. Any
 * other target type is treated as a programming error and surfaces as an
 * {@link IllegalStateException} so the misuse fails loudly during development.
 */
public class UniqueFileIdsValidator implements ConstraintValidator<UniqueFileIds, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value instanceof PresignedUrlPublicationRequest request) {
            List<PresignedUrlItem> files = request.files();
            if (files == null) {
                return true;
            }
            Set<String> uniqueIds = new HashSet<>();
            for (PresignedUrlItem item : files) {
                uniqueIds.add(item.id());
            }
            return uniqueIds.size() == files.size();
        }
        throw new IllegalStateException(
                "Unsupported type for @UniqueFileIds: " + value.getClass().getName()
        );
    }
}
