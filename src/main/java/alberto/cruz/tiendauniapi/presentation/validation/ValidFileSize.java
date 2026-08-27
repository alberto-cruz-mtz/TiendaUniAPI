package alberto.cruz.tiendauniapi.presentation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stub for the file-size validator that will land in PR 3.
 * The real {@code @Constraint(validatedBy = ValidFileSizeValidator.class)} wiring
 * is added when {@code ValidFileSizeValidator} is introduced.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface ValidFileSize {

    String message() default "El tamaño del archivo excede el máximo permitido para este tipo de contenido.";

    Class<?>[] groups() default {};

    Class<?>[] payload() default {};
}
