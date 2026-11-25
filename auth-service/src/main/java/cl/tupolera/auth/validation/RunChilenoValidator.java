package cl.tupolera.auth.validation;

import cl.tupolera.auth.util.RunUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RunChilenoValidator implements ConstraintValidator<RunChileno, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // Los valores vacíos los controla @NotBlank.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return RunUtil.esValido(valor);
    }
}
