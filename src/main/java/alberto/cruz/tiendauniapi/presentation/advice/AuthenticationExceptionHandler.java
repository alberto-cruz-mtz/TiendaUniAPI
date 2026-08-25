package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.EmailAddressAlreadyRegisteredException;
import alberto.cruz.tiendauniapi.service.exception.EmailAddressNotFound;
import alberto.cruz.tiendauniapi.service.exception.EmailDomainNotAllowedException;
import alberto.cruz.tiendauniapi.service.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    private static final String DOMAIN_URI = "https://tiendauniapi.com/problems";

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException() {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "No fue posible completar la autenticación. Por favor, verifique sus credenciales e intente nuevamente.");
        problemDetail.setTitle("Bad Credentials");
        problemDetail.setType(URI.create(DOMAIN_URI + "/bad-credentials"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler({EmailAddressNotFound.class, UsernameNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(RuntimeException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("User Not Found");
        problemDetail.setType(URI.create(DOMAIN_URI + "/user-not-found"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(EmailAddressAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAddressAlreadyRegisteredException(EmailAddressAlreadyRegisteredException exception) {
        HttpStatus status = HttpStatus.CONFLICT;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Email Address Already Registered");
        problemDetail.setType(URI.create(DOMAIN_URI + "/email-already-registered"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(EmailDomainNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleEmailDomainNotAllowedException(EmailDomainNotAllowedException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle("Email Domain Not Allowed");
        problemDetail.setType(URI.create(DOMAIN_URI + "/email-domain-not-allowed"));
        return ResponseEntity.status(status).body(problemDetail);
    }
}