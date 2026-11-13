package br.com.senac.recursos.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regras de texto dos cadastros (DEF-005 e DEF-010), conferidas por Bean Validation num bean de teste, sem Spring:
 * emoji, nome completo (proposta da spec) e e-mail de login. As mesmas anotações valem na API e nas telas.
 */
class ValidacaoDeTextoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void encerrar() {
        fabrica.close();
    }

    private record ComEmoji(@SemEmoji String texto) {
    }

    private record ComNome(@NomeCompleto String nome) {
    }

    private record ComEmail(@EmailDeLogin String email) {
    }

    private record NomeSemEmoji(@SemEmoji @NomeCompleto String nome) {
    }

    private record EmailSemEmoji(@SemEmoji @EmailDeLogin String email) {
    }

    private static boolean valido(Object bean) {
        return validador.validate(bean).isEmpty();
    }

    private static String mensagem(Object bean) {
        return validador.validate(bean).iterator().next().getMessage();
    }

    // ---------------------------------------------------------------- emoji

    @ParameterizedTest
    @ValueSource(strings = {
        "Ana 😀", "😀", "coração ❤️", "joia 👍🏽",
        "bandeira 🇧🇷", "tecla 1️⃣", "família 👩‍👩‍👧",
        "estrela ⭐", "sol ☀"
    })
    void textoComEmojiEhRecusadoComAMensagemDoCampo(String texto) {
        assertThat(Textos.contemEmoji(texto)).as(texto).isTrue();
        assertThat(valido(new ComEmoji(texto))).as(texto).isFalse();
        assertThat(mensagem(new ComEmoji(texto))).isEqualTo("Não use emojis neste campo.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Ana", "João D'Ávila", "Ñandú Çelik", "日本語", "ana@exemplo.com",
        "Sala 12 - Bloco B", "#1 * 2", "100% (ok)", ""
    })
    @NullSource
    void textoSemEmojiPassa(String texto) {
        assertThat(Textos.contemEmoji(texto)).as(texto).isFalse();
        assertThat(valido(new ComEmoji(texto))).as(texto).isTrue();
    }

    // ---------------------------------------------------------------- nome completo (proposta)

    @ParameterizedTest
    @ValueSource(strings = {
        "Ana Souza", "João da Silva", "Ana-Maria D'Ávila", "José A. Lima", "Maria J Silva",
        "Zoë Özdemir", "O’Brien Lee"
    })
    void nomeCompletoValidoPassa(String nome) {
        assertThat(valido(new ComNome(nome))).as(nome).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Ana", "A", "Ana1 Souza", "Ana Souza3", "-Ana Souza", "Ana @Souza", "Ana\tSouza", "Ana  Souza",
        " Ana Souza", "Ana Souza ", "Ana_Souza Lima", "Ana, Souza", "12 34"
    })
    void nomeSemSobrenomeComDigitoSimboloOuEspacoSoltoEhRecusado(String nome) {
        assertThat(valido(new ComNome(nome))).as("'" + nome + "'").isFalse();
        assertThat(mensagem(new ComNome(nome))).startsWith("Informe o nome completo");
    }

    @Test
    void nomeComAteCentoEVinteCaracteresPassaEComMaisNao() {
        String com120 = "Abcdefghi ".repeat(11) + "Abcdefghix"; // 11 x 10 + 10
        assertThat(com120).hasSize(120);
        assertThat(valido(new ComNome(com120))).isTrue();
        assertThat(valido(new ComNome(com120 + "x"))).isFalse();
    }

    @Test
    void nuloEVazioFicamParaONotBlankDoCampo() {
        assertThat(valido(new ComNome(null))).isTrue();
        assertThat(valido(new ComEmail(null))).isTrue();
    }

    @Test
    void nomeComEmojiRecebeSoAMensagemDoEmoji() {
        var violacoes = validador.validate(new NomeSemEmoji("Ana 😀 Souza"));
        assertThat(violacoes).extracting(v -> v.getMessage()).containsExactly("Não use emojis neste campo.");
    }

    @Test
    void normalizarNomeCortaAsPontasEReduzEspacosRepetidos() {
        assertThat(Textos.normalizarNome("  Ana \t  Souza\n")).isEqualTo("Ana Souza");
        assertThat(Textos.normalizarNome(null)).isNull();
        assertThat(Textos.nomeCompleto(Textos.normalizarNome("  ana    souza "))).isTrue();
    }

    // ---------------------------------------------------------------- e-mail de login

    @ParameterizedTest
    @ValueSource(strings = {
        "ana@exemplo.com", "ana.souza@exemplo.com.br", "ana+reservas@sub.exemplo.com", "a_b-c%d@ex-emplo.com",
        "a@b.co", "ana123@exemplo123.org"
    })
    void emailValidoPassa(String email) {
        assertThat(Emails.valido(email)).as(email).isTrue();
        assertThat(valido(new ComEmail(email))).as(email).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ana", "ana@", "@exemplo.com", "ana@exemplo", "ana@@exemplo.com", "ana@exemplo..com", "ana@.exemplo.com",
        "ana@exemplo.com.", ".ana@exemplo.com", "ana.@exemplo.com", "an..a@exemplo.com", "ana @exemplo.com",
        "ana@exe mplo.com", "ana@exemplo.com ", " ana@exemplo.com", "ana@-exemplo.com", "ana@exemplo-.com",
        "ana@exemplo.c", "ana@exemplo.com\n", "ana@exemplo.com,bia@exemplo.com", "<ana@exemplo.com>",
        "ana(a)@exemplo.com", "ana@exemplo.com/x", "ANA@EXEMPLO.COM", "Ana@exemplo.com", "ana@exemplo_x.com",
        "ana@çexemplo.com", "ana@[127.0.0.1]"
    })
    void emailForaDoFormatoEhRecusadoComAMensagemDoFormato(String email) {
        assertThat(Emails.valido(email)).as("'" + email + "'").isFalse();
        assertThat(valido(new ComEmail(email))).as("'" + email + "'").isFalse();
        assertThat(mensagem(new ComEmail(email))).isEqualTo("Informe um e-mail válido, como nome@dominio.com.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@exemplo.com😀", "ana😀@exemplo.com"})
    void emailComEmojiEhRecusadoPeloFormatoNaFuncaoEPeloEmojiNoCampo(String email) {
        assertThat(Emails.valido(email)).isFalse();
        assertThat(validador.validate(new EmailSemEmoji(email))).extracting(v -> v.getMessage())
                .containsExactly("Não use emojis neste campo.");
    }

    @Test
    void limiteDeTamanhoEhDe254CaracteresNoTodoE64NaParteLocal() {
        String local64 = "a".repeat(64);
        String dominio189 = "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(57) + ".com";
        String com254 = local64 + "@" + dominio189;
        assertThat(com254).hasSize(254);
        assertThat(Emails.TAMANHO_MAXIMO).isEqualTo(254);
        assertThat(Emails.valido(com254)).isTrue();
        assertThat(Emails.valido(com254 + "m")).isFalse();
        assertThat(Emails.valido("a".repeat(65) + "@exemplo.com")).isFalse();
        assertThat(Emails.valido("ana@" + "b".repeat(64) + ".com")).isFalse();
    }

    @Test
    void normalizarEmailCortaAsPontasEPoeEmMinusculasSemDependerDoIdiomaDoSistema() {
        Locale padrao = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR")); // em turco, "I".toLowerCase() daria "ı" sem Locale.ROOT
            assertThat(Emails.normalizar("  ANA.SILVA@EXEMPLO.COM \n")).isEqualTo("ana.silva@exemplo.com");
            assertThat(Emails.normalizar("INFO@IDEIA.COM")).isEqualTo("info@ideia.com");
        } finally {
            Locale.setDefault(padrao);
        }
        assertThat(Emails.normalizar(null)).isNull();
        assertThat(Emails.normalizar("   ")).isEmpty();
    }
}
