package alberto.cruz.tiendauniapi.service.helper;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3KeyGeneratorTest {

    private static final UUID USER_ID = UUID.fromString("4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c");

    private final S3KeyGenerator generator = new S3KeyGenerator();

    @Test
    void generateProfileKey_buildsProfilesFolderKeyWithJpegExtension() {
        String key = generator.generateProfileKey(USER_ID, "image/jpeg");

        String expectedPrefix = "profiles/" + USER_ID + "/";
        assertAll(
                () -> assertTrue(key.startsWith(expectedPrefix), () -> "Key should start with '" + expectedPrefix + "' but was: " + key),
                () -> assertTrue(key.endsWith(".jpg"), () -> "Key should end with '.jpg' but was: " + key)
        );

        String suffix = key.substring(expectedPrefix.length());
        String uuidPart = suffix.substring(0, suffix.lastIndexOf('.'));
        assertDoesNotThrow(() -> UUID.fromString(uuidPart), () -> "Middle part should be a valid UUID but was: " + uuidPart);
    }

    @Test
    void generatePublicationKey_buildsPublicationsFolderKeyWithMp4Extension() {
        String key = generator.generatePublicationKey(USER_ID, "video/mp4");

        String expectedPrefix = "publications/" + USER_ID + "/";
        assertAll(
                () -> assertTrue(key.startsWith(expectedPrefix), () -> "Key should start with '" + expectedPrefix + "' but was: " + key),
                () -> assertTrue(key.endsWith(".mp4"), () -> "Key should end with '.mp4' but was: " + key)
        );
    }

    @Test
    void generateProfileKey_throwsIllegalArgumentException_forUnknownMimeType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateProfileKey(USER_ID, "application/pdf")
        );

        assertEquals("Unsupported mime type: application/pdf", exception.getMessage());
    }
}