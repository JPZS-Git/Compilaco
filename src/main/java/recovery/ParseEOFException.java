package recovery;

/**
 * Lançada quando a recuperação de erros chega ao fim do arquivo sem
 * encontrar um token de sincronização.
 */
public class ParseEOFException extends Exception {
    public ParseEOFException(String x) {
        super(x);
    }
}
