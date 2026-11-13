package br.com.senac.recursos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere {@link Emails#valido}. Vazio e nulo passam (é o {@code @NotBlank} que exige o campo) e texto com emoji
 * também passa, porque quem recusa emoji é o {@link SemEmoji}: assim o campo mostra uma mensagem só.
 */
public class EmailDeLoginValidator implements ConstraintValidator<EmailDeLogin, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank() || Textos.contemEmoji(valor)) {
            return true;
        }
        return Emails.valido(valor);
    }
}
