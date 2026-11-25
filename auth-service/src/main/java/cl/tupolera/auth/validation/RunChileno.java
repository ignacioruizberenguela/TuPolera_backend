package cl.tupolera.auth.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Valida que el texto sea un RUN chileno con dígito verificador correcto. */
@Documented
@Constraint(validatedBy = RunChilenoValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface RunChileno {

    String message() default "El RUN no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
