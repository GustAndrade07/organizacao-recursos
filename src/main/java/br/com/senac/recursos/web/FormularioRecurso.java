package br.com.senac.recursos.web;

/**
 * Dados do formulário de recurso (tela 14). Em "novo" voltam da própria URL (a troca de tipo sem JavaScript
 * recarrega a página levando o que já foi digitado); em edição é o back-end que preenche, com o {@code id}
 * do recurso. Os campos que o tipo não usa ficam nulos. A capacidade é texto para o formulário poder
 * devolver o que o usuário digitou, mesmo inválido.
 */
public record FormularioRecurso(String id, String nome, String capacidade, String localizacao, String patrimonio,
                                String competencias, boolean restrito) {
}
