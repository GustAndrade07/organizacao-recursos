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

/** E-mail usado como login (DEF-010): formato de {@link Emails}. Nulo e vazio ficam com {@code @NotBlank}. */
@Documented
@Constraint(validatedBy = EmailDeLoginValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailDeLogin {

    String message() default "Informe um e-mail válido, como nome@dominio.com.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
