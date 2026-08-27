package alberto.cruz.tiendauniapi.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UniqueFileIdsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueFileIds {

    String message() default "Los ids de los archivos deben ser únicos dentro del mismo request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
