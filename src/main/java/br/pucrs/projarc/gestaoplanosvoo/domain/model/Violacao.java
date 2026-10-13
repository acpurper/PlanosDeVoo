package br.pucrs.projarc.gestaoplanosvoo.domain.model;

/** Uma inconsistência encontrada na validação. {@code trecho} é a ordem do trecho, ou null se for do plano inteiro. */
public record Violacao(CodigoViolacao codigo, Integer trecho, String mensagem) {
}
