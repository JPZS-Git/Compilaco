package parser;

import java.io.IOException;
import java.util.List;

/**
 * Analisador léxico com recuperação de erros (modo pânico no léxico).
 *
 * Estende o léxico gerado pelo JavaCC. Quando aparece um símbolo que não
 * pertence à linguagem (ex.: '@'), em vez de parar a compilação:
 *   1. registra o erro léxico na lista de erros;
 *   2. descarta só o caractere inválido;
 *   3. continua a leitura a partir do caractere seguinte.
 *
 * Assim o parser nunca recebe o símbolo inválido e a análise segue,
 * reportando todos os erros numa única compilação.
 *
 * Fica fora do Compilaco.jj, então não é sobrescrita ao regenerar o parser.
 */
public class LexicoComRecuperacao extends CompilacoTokenManager {

    private final List<ErroCompilacao> erros;

    public LexicoComRecuperacao(SimpleCharStream entrada, List<ErroCompilacao> erros) {
        super(entrada);
        this.erros = erros;
    }

    @Override
    public Token getNextToken() {
        while (true) {
            try {
                return super.getNextToken();
            } catch (TokenMgrError e) {
                registrarErro();
                pularCaractereInvalido();
            }
        }
    }

    /** Registra o erro na posição onde o lexema inválido começou. */
    private void registrarErro() {
        int linha = input_stream.getBeginLine();
        int coluna = input_stream.getBeginColumn();
        char c = input_stream.buffer[input_stream.tokenBegin];

        String mensagem;
        if (c == '"') {
            mensagem = "Narração (string) aberta e nunca fechada: faltou fechar as aspas.";
        } else {
            mensagem = "Símbolo inválido: '" + exibir(c)
                    + "'. Esse símbolo não pertence ao alfabeto da linguagem Compilaço.";
        }
        erros.add(new ErroCompilacao(TipoErro.LEXICO, linha, coluna, mensagem));
    }

    /**
     * Deixa a leitura logo depois do primeiro caractere do lexema inválido.
     *
     * Depois do erro, o léxico do JavaCC pode ter parado antes desse
     * caractere (caso de um símbolo sozinho, como '@') ou depois dele
     * (caso de um começo de token que não se completou, como '!' sem '='
     * ou uma string sem aspas de fechamento). Nos dois casos a leitura
     * volta para o ponto certo, e o resto do código é lido normalmente.
     */
    private void pularCaractereInvalido() {
        int distancia = input_stream.bufpos - input_stream.tokenBegin;
        if (distancia < -1) {
            distancia += input_stream.bufsize; // buffer circular
        }

        if (distancia == -1 || distancia == input_stream.bufsize - 1) {
            try {
                input_stream.readChar();
            } catch (IOException fimDoArquivo) {
                // nada a pular: o próximo token será o <EOF>
            }
        } else if (distancia > 0) {
            input_stream.backup(distancia);
        }
    }

    private static String exibir(char c) {
        if (c >= 0x20 && c != 0x7f) {
            return String.valueOf(c);
        }
        return switch (c) {
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            default -> String.format("\\u%04x", (int) c);
        };
    }
}
