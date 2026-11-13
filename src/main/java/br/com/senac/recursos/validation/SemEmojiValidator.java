package br.com.senac.recursos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Recusa texto com emoji ({@link Textos#contemEmoji}). Nulo passa: quem exige o campo é o {@code @NotBlank}. */
public class SemEmojiValidator implements ConstraintValidator<SemEmoji, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return !Textos.contemEmoji(valor);
    }
}
