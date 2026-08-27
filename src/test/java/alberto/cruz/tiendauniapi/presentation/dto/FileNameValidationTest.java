package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documents the {@code fileName} validation contract shared by
 * {@link PresignedUrlProfileRequest} and {@link PresignedUrlItem}. The Jakarta
 * annotations ({@code @NotBlank}, {@code @Pattern(regexp = "^[a-zA-Z0-9_-]+$")},
 * {@code @Size(max = 255)}) are declared on both records; this suite pins the
 * contract so a future refactor cannot silently relax it.
 */
class FileNameValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("emptyFileName_fails: empty fileName triggers a fileName violation on PresignedUrlProfileRequest")
    void emptyFileName_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("empty fileName must produce a violation")
                .isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("fileNameWithSpace_fails: fileName containing a space violates the regex")
    void fileNameWithSpace_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("con espacio", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("fileNameWithDot_fails: fileName containing a dot violates the regex")
    void fileNameWithDot_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("con.punto", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("fileNameWithPathTraversal_fails: path traversal segments violate the regex")
    void fileNameWithPathTraversal_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("../path", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("nullFileName_fails: null fileName triggers a fileName violation")
    void nullFileName_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(null, 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("validFileName_passes: alphanumeric + dash fileName produces zero violations")
    void validFileName_passes() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("demo-front", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("validFileNameWithUnderscore_passes: alphanumeric + underscore fileName produces zero violations")
    void validFileNameWithUnderscore_passes() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest("test_123", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("tooLongFileName_fails: fileName > 255 chars triggers the @Size(max=255) violation")
    void tooLongFileName_fails() {
        String tooLong = "a".repeat(256);
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(tooLong, 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("presignedUrlItem_fileNameWithPathTraversal_fails: same regex applies to publication items")
    void presignedUrlItem_fileNameWithPathTraversal_fails() {
        PresignedUrlItem item = new PresignedUrlItem("file-1", "../escape", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlItem>> violations = validator.validate(item);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("presignedUrlItem_validFileName_passes: well-formed fileName on publication item passes")
    void presignedUrlItem_validFileName_passes() {
        PresignedUrlItem item = new PresignedUrlItem("file-1", "front_view", 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlItem>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("presignedUrlItem_tooLongFileName_fails: @Size(max=255) also applies to publication items")
    void presignedUrlItem_tooLongFileName_fails() {
        String tooLong = "a".repeat(256);
        PresignedUrlItem item = new PresignedUrlItem("file-1", tooLong, 1024L, "image/jpeg");

        Set<ConstraintViolation<PresignedUrlItem>> violations = validator.validate(item);

        assertThat(violations)
                .anyMatch(v -> "fileName".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("docs: fileName regex accepts letters, digits, underscores and dashes only")
    void documentsAcceptedCharacterSet() {
        // This test is a no-op assertion that keeps the accepted character set
        // visible in code review. It mirrors the @Pattern declaration on both DTOs.
        String acceptedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";
        assertThat(acceptedChars).matches("^[a-zA-Z0-9_-]+$");
        assertThat(List.of("con espacio", "con.punto", "../path", "")).noneMatch("^[a-zA-Z0-9_-]+$"::matches);
    }
}
