package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlItem;
import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlPublicationRequest;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueFileIdsValidatorTest {

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
    @DisplayName("uniqueIds_passes: 3 distinct ids produce no uniqueness violation")
    void uniqueIds_passes() {
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(
                new PresignedUrlItem("file1", "front", 1024L, "image/jpeg"),
                new PresignedUrlItem("uuid-string", "demo", 1024L, "image/png"),
                new PresignedUrlItem("another", "side", 1024L, "image/webp")
        ));

        Set<ConstraintViolation<PresignedUrlPublicationRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().noneMatch(v -> v.getMessage().toLowerCase().contains("ids")),
                "no uniqueness-related violations expected; got: " + violations
        );
    }

    @Test
    @DisplayName("duplicateIds_fails: two items sharing id file1 produces a uniqueness violation")
    void duplicateIds_fails() {
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(
                new PresignedUrlItem("file1", "front", 1024L, "image/jpeg"),
                new PresignedUrlItem("file1", "front", 1024L, "image/jpeg"),
                new PresignedUrlItem("file2", "side", 1024L, "image/webp")
        ));

        Set<ConstraintViolation<PresignedUrlPublicationRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("ids")),
                "expected uniqueness-related violation; got: " + violations
        );
    }

    @Test
    @DisplayName("singleFile_passes: a single item trivially has no duplicates")
    void singleFile_passes() {
        PresignedUrlPublicationRequest request = new PresignedUrlPublicationRequest(List.of(
                new PresignedUrlItem("file1", "front", 1024L, "image/jpeg")
        ));

        Set<ConstraintViolation<PresignedUrlPublicationRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().noneMatch(v -> v.getMessage().toLowerCase().contains("ids")),
                "no uniqueness-related violations expected; got: " + violations
        );
    }
}
