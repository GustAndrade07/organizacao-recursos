package br.com.senac.recursos.web;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tipos de recurso do cadastro (telas 13 e 14): sala, material e professor. A {@code chave} é o valor do
 * parâmetro {@code tipo} nas duas telas ({@code salas}, {@code materiais}, {@code professores}); os demais
 * textos são os rótulos da tela, já no gênero e número certos, e {@code campos} diz quais campos de
 * formulário o tipo mostra.
 */
public enum TipoRecurso {

    SALAS("salas", "Sala", "Nome do recurso", "Nenhuma sala cadastrada", "Salas cadastradas", "Buscar salas por nome", "nome", "capacidade", "localizacao"),
    MATERIAIS("materiais", "Material", "Nome do recurso", "Nenhum material cadastrado", "Materiais cadastrados", "Buscar materiais por nome", "nome", "patrimonio", "localizacao"),
    PROFESSORES("professores", "Professor", "Nome do professor", "Nenhum professor cadastrado", "Professores cadastrados", "Buscar professores por nome", "nome", "competencias");

    private final String chave;
    private final String singular;
    private final String rotuloNome;
    private final String semRecursos;
    private final String legenda;
    private final String rotuloBusca;
    private final List<String> campos;

    TipoRecurso(String chave, String singular, String rotuloNome, String semRecursos, String legenda, String rotuloBusca,
               String... campos) {
        this.chave = chave;
        this.singular = singular;
        this.rotuloNome = rotuloNome;
        this.semRecursos = semRecursos;
        this.legenda = legenda;
        this.rotuloBusca = rotuloBusca;
        this.campos = List.of(campos);
    }

    /** Aceita só as três chaves; qualquer outro valor (ou nenhum) cai em salas. */
    public static TipoRecurso da(String chave) {
        for (TipoRecurso tipo : values()) {
            if (tipo.chave.equals(chave)) {
                return tipo;
            }
        }
        return SALAS;
    }

    /** Para os formulários: sem {@code tipo} vale salas, mas um valor desconhecido é pedido inválido (400). */
    public static TipoRecurso exigir(String chave) {
        if (chave == null || chave.isBlank()) {
            return SALAS;
        }
        for (TipoRecurso tipo : values()) {
            if (tipo.chave.equals(chave)) {
                return tipo;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de recurso desconhecido");
    }

    /** Quantos dos erros são de campos que o formulário deste tipo mostra (o resumo conta só esses). */
    public long contarCampos(Map<String, String> erros) {
        return erros == null ? 0 : erros.keySet().stream().filter(campos::contains).count();
    }

    public List<String> getCampos() {
        return campos;
    }

    public String getChave() {
        return chave;
    }

    public String getSingular() {
        return singular;
    }

    public String getRotuloNome() {
        return rotuloNome;
    }

    public String getSemRecursos() {
        return semRecursos;
    }

    public String getLegenda() {
        return legenda;
    }

    public String getRotuloBusca() {
        return rotuloBusca;
    }
}
