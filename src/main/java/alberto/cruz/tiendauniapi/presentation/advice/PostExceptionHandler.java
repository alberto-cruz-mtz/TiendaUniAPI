package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.PostAlreadyPublishedException;
import alberto.cruz.tiendauniapi.service.exception.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class PostExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePostNotFoundException(PostNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Post Not Found");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/post-not-found"));

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(PostAlreadyPublishedException.class)
    public ResponseEntity<ProblemDetail> handlePostAlreadyPublishedException(PostAlreadyPublishedException ex) {
        HttpStatus status = HttpStatus.CONFLICT;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Post Already Published");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/post-already-published"));

        return ResponseEntity.status(status).body(problemDetail);
    }
}
