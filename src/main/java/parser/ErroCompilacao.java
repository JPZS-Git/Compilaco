package parser;

import java.util.Objects;

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
}