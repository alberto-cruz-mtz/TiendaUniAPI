package alberto.cruz.tiendauniapi.service.exception;

/**
 * Raised when the AWS SDK fails to produce a presigned URL. The original cause is
 * preserved for server-side logging while the public message stays neutral so the
 * {@code PresignedUrlExceptionHandler} can render its own 503 ProblemDetail without
 * leaking SDK internals.
 */
public class PresignedUrlGenerationException extends RuntimeException {

    public PresignedUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}