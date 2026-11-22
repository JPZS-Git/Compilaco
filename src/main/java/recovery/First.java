package recovery;

import parser.CompilacoConstants;

/**
 * Conjuntos First dos não-terminais do Compilaço usados na recuperação.
 *
 * First(X) = tokens que podem iniciar X.
 */
public class First {

    static public final RecoverySet tipoDado = new RecoverySet();
    static public final RecoverySet declaraVar = tipoDado;       // começa pelo tipo
    static public final RecoverySet atribuicao = new RecoverySet();
    static public final RecoverySet comandoIf = new RecoverySet();
    static public final RecoverySet comandoWhile = new RecoverySet();
    static public final RecoverySet comandoFor = new RecoverySet();
    static public final RecoverySet comando = new RecoverySet();
    static public final RecoverySet bloco = new RecoverySet();

    static {
        tipoDado.add(CompilacoConstants.TIPOINT);        // placar
        tipoDado.add(CompilacoConstants.TIPOFLOAT);      // estatistica
        tipoDado.add(CompilacoConstants.TIPOSTRING);     // narracao

        atribuicao.add(CompilacoConstants.IDENTIFICADOR);

        comandoIf.add(CompilacoConstants.IF);            // var_analisa
        comandoWhile.add(CompilacoConstants.WHILE);      // enquanto_bola_rolar
        comandoFor.add(CompilacoConstants.FOR);          // campeonato

        // comando -> declaraVar | atribuicao | comandoIf | comandoWhile | comandoFor
        comando.addAll(declaraVar);
        comando.addAll(atribuicao);
        comando.addAll(comandoIf);
        comando.addAll(comandoWhile);
        comando.addAll(comandoFor);

        bloco.add(CompilacoConstants.ABREBLOCO);         // {
    }
}
