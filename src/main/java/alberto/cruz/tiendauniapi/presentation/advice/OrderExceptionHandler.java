package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.InsufficientProductStockException;
import alberto.cruz.tiendauniapi.service.exception.OrderNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductPriceChangedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class OrderExceptionHandler {


    @ExceptionHandler(InsufficientProductStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientProductStockException(InsufficientProductStockException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Insufficient Product Stock");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/insufficient-product-stock"));

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ProductPriceChangedException.class)
    public ResponseEntity<ProblemDetail> handleProductPriceChangedException(ProductPriceChangedException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Product Price Changed");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/product-price-changed"));

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFoundException(OrderNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Order Not Found");
        problemDetail.setType(URI.create(GlobalExceptionHandler.DOMAIN_URI + "/order-not-found"));

        return ResponseEntity.status(status).body(problemDetail);
    }
}
