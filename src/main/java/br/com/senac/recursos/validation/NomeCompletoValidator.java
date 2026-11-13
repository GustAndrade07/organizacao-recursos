package br.com.senac.recursos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Esqueleto do TDD: ainda não recusa nada. */
public class NomeCompletoValidator implements ConstraintValidator<NomeCompleto, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return true;
    }
}
