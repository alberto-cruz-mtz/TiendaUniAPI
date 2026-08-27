package alberto.cruz.tiendauniapi.presentation.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compile-and-shape smoke test for the presigned URL DTOs.
 *
 * <p>Pure data carriers (records) don't justify strict RED/GREEN, but the wiring
 * matters: a future refactor that drops a component breaks every controller and
 * service down the line. This test pins each record's component count and the
 * accessors the slice relies on, so accidental drift fails fast.
 */
class DtoCompilationSmokeTest {

    @Test
    void presignedUrlItem_carriesAllFourComponents() {
        PresignedUrlItem item = new PresignedUrlItem("id-1", "avatar", 1024L, "image/jpeg");

        assertEquals("id-1", item.id());
        assertEquals("avatar", item.fileName());
        assertEquals(1024L, item.size());
        assertEquals("image/jpeg", item.mimeType());
    }

    @Test
    void presignedUrlProfileRequest_carriesAllThreeComponents() {
        PresignedUrlProfileRequest request =
                new PresignedUrlProfileRequest("avatar", 1024L, "image/jpeg");

        assertEquals("avatar", request.fileName());
        assertEquals(1024L, request.size());
        assertEquals("image/jpeg", request.mimeType());
    }

    @Test
    void presignedUrlProfileResponse_carriesUrl() {
        PresignedUrlProfileResponse response =
                new PresignedUrlProfileResponse("https://example.com/signed");

        assertEquals("https://example.com/signed", response.url());
    }

    @Test
    void updateAvatarKeyRequest_carriesKey() {
        String key = "profiles/4f1c2b8a-3d4e-5f6a-7b8c-9d0e1f2a3b4c/8a3e2c11-1111-2222-3333-444455556666.jpg";

        UpdateAvatarKeyRequest request = new UpdateAvatarKeyRequest(key);

        assertEquals(key, request.key());
    }

    @Test
    void presignedUrlPublicationRequest_carriesFiles() {
        PresignedUrlItem first = new PresignedUrlItem("a", "front", 1024L, "image/jpeg");
        PresignedUrlItem second = new PresignedUrlItem("b", "demo", 2048L, "image/png");

        PresignedUrlPublicationRequest request =
                new PresignedUrlPublicationRequest(List.of(first, second));

        assertNotNull(request.files());
        assertEquals(2, request.files().size());
        assertEquals("a", request.files().get(0).id());
        assertEquals("b", request.files().get(1).id());
    }

    @Test
    void presignedUrlPublicationResponse_carriesUris() {
        PresignedUrlItemResponse item =
                new PresignedUrlItemResponse("a", "https://example.com/signed", "publications/x/y.jpg");

        PresignedUrlPublicationResponse response = new PresignedUrlPublicationResponse(List.of(item));

        assertEquals(1, response.uris().size());
        assertEquals("a", response.uris().get(0).id());
        assertEquals("https://example.com/signed", response.uris().get(0).url());
        assertEquals("publications/x/y.jpg", response.uris().get(0).key());
    }

    @Test
    void presignedUrlItemResponse_carriesIdUrlAndKey() {
        PresignedUrlItemResponse response =
                new PresignedUrlItemResponse("id-1", "https://example.com/signed", "publications/x/y.jpg");

        assertEquals("id-1", response.id());
        assertEquals("https://example.com/signed", response.url());
        assertEquals("publications/x/y.jpg", response.key());
    }

    @Test
    void allDtoClasses_canBeLoaded() {
        assertDoesNotThrow(() -> Class.forName(PresignedUrlItem.class.getName()));
        assertDoesNotThrow(() -> Class.forName(PresignedUrlProfileRequest.class.getName()));
        assertDoesNotThrow(() -> Class.forName(PresignedUrlProfileResponse.class.getName()));
        assertDoesNotThrow(() -> Class.forName(UpdateAvatarKeyRequest.class.getName()));
        assertDoesNotThrow(() -> Class.forName(PresignedUrlPublicationRequest.class.getName()));
        assertDoesNotThrow(() -> Class.forName(PresignedUrlPublicationResponse.class.getName()));
        assertDoesNotThrow(() -> Class.forName(PresignedUrlItemResponse.class.getName()));
    }

    @Test
    void recordComponentsAreNotNull_afterConstruction() {
        PresignedUrlItem item = new PresignedUrlItem("id", "name", 1L, "image/jpeg");
        assertTrue(item.id() != null);
        assertTrue(item.fileName() != null);
        assertTrue(item.size() != null);
        assertTrue(item.mimeType() != null);
    }
}
