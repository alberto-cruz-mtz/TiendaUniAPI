package alberto.cruz.tiendauniapi.presentation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stub for the unique-file-ids validator that will land in PR 3.
 * The real {@code @Constraint(validatedBy = UniqueFileIdsValidator.class)} wiring
 * is added when {@code UniqueFileIdsValidator} is introduced.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface UniqueFileIds {

    String message() default "Los siguientes ids están duplicados.";

    Class<?>[] groups() default {};

    Class<?>[] payload() default {};
}
