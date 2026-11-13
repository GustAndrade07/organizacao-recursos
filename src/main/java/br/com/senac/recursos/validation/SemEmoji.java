package br.com.senac.recursos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/** Campo de texto sem emoji (DEF-005). Único validador de emoji do sistema: API e telas usam esta anotação. */
@Documented
@Constraint(validatedBy = SemEmojiValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SemEmoji {

    String message() default "Não use emojis neste campo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
