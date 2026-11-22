package recovery;

import parser.CompilacoConstants;

/**
 * Conjuntos de sincronização usados no consumeUntil.
 *
 * Seguem a regra do modo pânico (Aula 10): os tokens de sincronismo de um
 * não-terminal A são os elementos de Follow(A), isto é, os tokens que podem
 * aparecer logo depois de A. Ao encontrar um erro dentro de A, o parser
 * descarta tokens até achar um deles e continua a análise a partir dali.
 */
public class Follow {

    /** Follow(comando) = First(comando) ∪ { "}", "fim_de_jogo" } */
    static public final RecoverySet comando = First.comando
            .union(new RecoverySet(CompilacoConstants.FECHABLOCO))
            .union(new RecoverySet(CompilacoConstants.FECHAMAIN));

    /**
     * Condição do var_analisa / enquanto_bola_rolar / campeonato:
     * Follow(condição) = { "{" }, o início do bloco.
     * Como segurança, caso o "{" também esteja faltando, entram os tokens que
     * nunca aparecem dentro de uma condição: "}", "fim_de_jogo" e o início
     * dos comandos de controle. (Tipos e identificadores ficam de fora, porque
     * aparecem dentro do cabeçalho do campeonato.)
     */
    static public final RecoverySet condicao = First.bloco
            .union(new RecoverySet(CompilacoConstants.FECHABLOCO))
            .union(new RecoverySet(CompilacoConstants.FECHAMAIN))
            .union(First.comandoIf)
            .union(First.comandoWhile)
            .union(First.comandoFor);

    /** Depois de "apito_inicial()": os comandos ou o "fim_de_jogo". */
    static public final RecoverySet cabecalho = First.comando
            .union(new RecoverySet(CompilacoConstants.FECHAMAIN));

    /**
     * Nível do programa (token fora de qualquer comando, ex.: um "}" sobrando):
     * sincroniza no próximo comando, no "fim_de_jogo" ou no fim do arquivo,
     * para que o resto do programa continue sendo analisado.
     */
    static public final RecoverySet programa = First.comando
            .union(new RecoverySet(CompilacoConstants.FECHAMAIN))
            .union(new RecoverySet(CompilacoConstants.EOF));
}
