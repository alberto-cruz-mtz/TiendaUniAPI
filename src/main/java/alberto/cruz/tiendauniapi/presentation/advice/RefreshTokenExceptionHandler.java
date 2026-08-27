package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.ExpiredRefreshTokenException;
import alberto.cruz.tiendauniapi.service.exception.InvalidRefreshTokenException;
import alberto.cruz.tiendauniapi.service.exception.RefreshTokenNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.RevokedRefreshTokenException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class RefreshTokenExceptionHandler {

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshTokenException(InvalidRefreshTokenException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Invalid Refresh Token");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/invalid-refresh-token"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRefreshTokenNotFoundException(RefreshTokenNotFoundException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Refresh Token Not Found");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/refresh-token-not-found"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleExpiredRefreshTokenException(ExpiredRefreshTokenException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Expired Refresh Token");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/expired-refresh-token"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(RevokedRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleRevokedRefreshTokenException(RevokedRefreshTokenException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Revoked Refresh Token");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/revoked-refresh-token"));
        return ResponseEntity.status(status).body(problemDetail);
    }

}