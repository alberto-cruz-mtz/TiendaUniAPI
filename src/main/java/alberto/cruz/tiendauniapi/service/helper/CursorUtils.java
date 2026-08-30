package alberto.cruz.tiendauniapi.service.helper;

import alberto.cruz.tiendauniapi.service.model.Cursor;
import alberto.cruz.tiendauniapi.service.model.PostId;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class CursorUtils {

    public static Cursor decodeCursor(String cursor) {
        if(cursor == null) return new Cursor(null, null);

        byte[] cursorBytes = cursor.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = Base64.getUrlDecoder().decode(cursorBytes);
        String decodedCursor = new String(bytes, StandardCharsets.UTF_8);

        String[] parts = decodedCursor.split("\\|");

        if (parts.length < 2) {
            throw new IllegalArgumentException("El cursor no tiene el formato esperado.");
        }

        String postIdString = parts[0];
        PostId postId = new PostId(postIdString);

        String postedAtString = parts[1];
        Instant postedAt = Instant.parse(postedAtString);

        return new Cursor(postId.value(), postedAt);
    }

    public static String encodeCursor(UUID postId, Instant postedAt) {
        String postIdString = postId.toString();
        String postedAtString = postedAt.toString();
        String combined = postIdString + "|" + postedAtString;

        byte[] bytes = combined.getBytes(StandardCharsets.UTF_8);
        byte[] encodedBytes = Base64.getUrlEncoder().encode(bytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }
}
