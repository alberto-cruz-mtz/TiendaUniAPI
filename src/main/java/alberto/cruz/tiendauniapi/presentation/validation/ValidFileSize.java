package alberto.cruz.tiendauniapi.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidFileSizeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileSize {

    String message() default "El tamaño del archivo excede el límite permitido para este tipo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
