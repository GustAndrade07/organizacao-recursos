package br.com.senac.recursos.web;

/**
 * Tipos de recurso do cadastro (telas 13 e 14): sala, material e professor. A {@code chave} é o valor do
 * parâmetro {@code tipo} nas duas telas ({@code salas}, {@code materiais}, {@code professores}); os demais
 * textos são os rótulos da tela, já no gênero e número certos.
 */
public enum TipoRecurso {

    SALAS("salas", "Sala", "Nome do recurso", "Nenhuma sala cadastrada", "Salas cadastradas", "Buscar salas por nome"),
    MATERIAIS("materiais", "Material", "Nome do recurso", "Nenhum material cadastrado", "Materiais cadastrados", "Buscar materiais por nome"),
    PROFESSORES("professores", "Professor", "Nome do professor", "Nenhum professor cadastrado", "Professores cadastrados", "Buscar professores por nome");

    private final String chave;
    private final String singular;
    private final String rotuloNome;
    private final String semRecursos;
    private final String legenda;
    private final String rotuloBusca;

    TipoRecurso(String chave, String singular, String rotuloNome, String semRecursos, String legenda, String rotuloBusca) {
        this.chave = chave;
        this.singular = singular;
        this.rotuloNome = rotuloNome;
        this.semRecursos = semRecursos;
        this.legenda = legenda;
        this.rotuloBusca = rotuloBusca;
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
