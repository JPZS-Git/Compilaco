package parser;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record ErroCompilacao(
        TipoErro tipo,
        int linha,
        int coluna,
        String mensagem
) {
    public ErroCompilacao {
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(mensagem);

        if (linha < 0 || coluna < 0) {
            throw new IllegalArgumentException(
                    "Linha e coluna não podem ser negativas."
            );
        }
    }

    /**
     * Converte uma ParseException do JavaCC em erro sintático:
     * posição do token inesperado, o que foi encontrado e o que era esperado.
     */
    public static ErroCompilacao sintatico(ParseException erro) {
        Token token = erro.currentToken != null
                ? erro.currentToken.next
                : null;

        int linha = token != null ? token.beginLine : 0;
        int coluna = token != null ? token.beginColumn : 0;

        String encontrado = token == null
                ? "desconhecido"
                : token.kind == CompilacoConstants.EOF
                  ? "<EOF>"
                  : "'" + token.image + "'";

        Set<String> esperados = new LinkedHashSet<>();

        if (erro.expectedTokenSequences != null) {
            for (int[] sequencia : erro.expectedTokenSequences) {
                if (sequencia.length > 0) {
                    esperados.add(CompilacoConstants.tokenImage[sequencia[0]]);
                }
            }
        }

        String mensagem = "Token inesperado: " + encontrado + ".";

        if (!esperados.isEmpty()) {
            mensagem += " Esperado: " + String.join(", ", esperados) + ".";
        }

        return new ErroCompilacao(TipoErro.SINTATICO, linha, coluna, mensagem);
    }
}
