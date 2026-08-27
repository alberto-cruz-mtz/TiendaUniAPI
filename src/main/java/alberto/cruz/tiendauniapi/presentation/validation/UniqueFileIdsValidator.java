package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class-level validator that enforces id uniqueness across the {@code files} list of a
 * {@link PresignedUrlPublicationRequest}.
 *
 * <p>The check walks the list, counts occurrences per id, and surfaces the duplicated
 * ids in the violation message so the global exception handler can echo them back to
 * the client (AC-POST-3). Any other target type is treated as a programming error and
 * surfaces as an {@link IllegalStateException} so the misuse fails loudly during
 * development.
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
            Map<String, Long> idOccurrences = files.stream()
                    .collect(Collectors.groupingBy(PresignedUrlItem::id, HashMap::new, Collectors.counting()));

            List<String> duplicateIds = idOccurrences.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();

            if (duplicateIds.isEmpty()) {
                return true;
            }

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Los siguientes ids están duplicados: " + String.join(", ", duplicateIds))
                    .addConstraintViolation();
            return false;
        }
        throw new IllegalStateException(
                "Unsupported type for @UniqueFileIds: " + value.getClass().getName()
        );
    }
}
