package alberto.cruz.tiendauniapi.presentation.validation;

import alberto.cruz.tiendauniapi.presentation.dto.PresignedUrlProfileRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidFileSizeValidatorTest {

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
    @DisplayName("imageWithinCap_passes: image/jpeg at exact 10MB cap is valid")
    void imageWithinCap_passes() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "avatar",
                10_485_760L,
                "image/jpeg"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().noneMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "no size-related violations expected; got: " + violations
        );
    }

    @Test
    @DisplayName("imageOverCap_fails: image/jpeg one byte over the 10MB cap fails")
    void imageOverCap_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "avatar",
                10_485_761L,
                "image/jpeg"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "expected a size-related violation; got: " + violations
        );
    }

    @Test
    @DisplayName("videoWithinCap_passes: video/mp4 at exact 50MB cap is valid")
    void videoWithinCap_passes() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "demo",
                52_428_800L,
                "video/mp4"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().noneMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "no size-related violations expected; got: " + violations
        );
    }

    @Test
    @DisplayName("videoOverCap_fails: video/mp4 one byte over the 50MB cap fails")
    void videoOverCap_fails() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "demo",
                52_428_801L,
                "video/mp4"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "expected a size-related violation; got: " + violations
        );
    }

    @Test
    @DisplayName("svgTreatedAsImage_failsOver10Mb: image/svg+xml > 10MB uses image cap, not video cap")
    void svgTreatedAsImage_failsOver10Mb() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "logo",
                11_000_000L,
                "image/svg+xml"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "expected a size-related violation; got: " + violations
        );
    }

    @Test
    @DisplayName("nullSize_isValid: @ValidFileSize delegates non-positive sizes to @Positive")
    void nullSize_isValid() {
        PresignedUrlProfileRequest request = new PresignedUrlProfileRequest(
                "avatar",
                null,
                "image/jpeg"
        );

        Set<ConstraintViolation<PresignedUrlProfileRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream().noneMatch(v -> v.getMessage().toLowerCase().contains("tamaño")),
                "no size-related violations expected from @ValidFileSize; got: " + violations
        );
    }
}
