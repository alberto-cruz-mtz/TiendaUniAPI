package alberto.cruz.tiendauniapi.service.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the contract between {@link PresignedUrlMimeExtension#WHITELIST_REGEX} and the
 * enum values. The DTO {@code @Pattern} validators depend on the regex; if anyone adds
 * a new mime type to the enum, this test fails unless the constant is regenerated.
 */
class PresignedUrlMimeExtensionTest {

    @Test
    void whitelistRegex_acceptsEveryEnumMimeTypeCaseInsensitive() {
        Pattern compiled = Pattern.compile(
                PresignedUrlMimeExtension.WHITELIST_REGEX, Pattern.CASE_INSENSITIVE);

        for (PresignedUrlMimeExtension entry : PresignedUrlMimeExtension.values()) {
            assertTrue(
                    compiled.matcher(entry.mimeType()).matches(),
                    () -> "WHITELIST_REGEX must accept the canonical mimeType " + entry.mimeType()
            );
        }
    }

    @Test
    void whitelistRegex_rejectsMimeTypesOutsideTheEnum() {
        Pattern compiled = Pattern.compile(
                PresignedUrlMimeExtension.WHITELIST_REGEX, Pattern.CASE_INSENSITIVE);

        assertFalse(compiled.matcher("text/plain").matches());
        assertFalse(compiled.matcher("application/json").matches());
        assertFalse(compiled.matcher("image/bmp").matches());
    }

    @Test
    void whitelistRegex_isCaseInsensitive() {
        Pattern compiled = Pattern.compile(
                PresignedUrlMimeExtension.WHITELIST_REGEX, Pattern.CASE_INSENSITIVE);

        assertTrue(compiled.matcher("IMAGE/JPEG").matches());
        assertTrue(compiled.matcher("Image/Svg+Xml").matches());
    }

    @Test
    void whitelistRegex_escapesPlusSignInSvg() {
        // image/svg+xml contains a literal '+' which must be escaped, not treated as a quantifier.
        assertTrue(
                PresignedUrlMimeExtension.WHITELIST_REGEX.contains("svg\\+xml"),
                () -> "Expected WHITELIST_REGEX to escape the '+' in svg+xml, got: "
                        + PresignedUrlMimeExtension.WHITELIST_REGEX
        );
    }

    @Test
    void whitelistSet_staysInSyncWithEnumValues() {
        Set<String> whitelist = PresignedUrlMimeExtension.whitelist();
        assertEquals(PresignedUrlMimeExtension.values().length, whitelist.size());
        for (PresignedUrlMimeExtension entry : PresignedUrlMimeExtension.values()) {
            assertTrue(whitelist.contains(entry.mimeType()));
        }
    }
}
