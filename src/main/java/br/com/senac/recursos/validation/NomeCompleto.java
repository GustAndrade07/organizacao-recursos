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

/** Nome completo (regra PROPOSTA, falta o grupo aprovar): ver {@link Textos#nomeCompleto(String)}. Nulo e vazio ficam com {@code @NotBlank}. */
@Documented
@Constraint(validatedBy = NomeCompletoValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NomeCompleto {

    String message() default "Informe o nome completo, com nome e sobrenome (só letras, espaço, apóstrofo, hífen e ponto, até 120 caracteres).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
