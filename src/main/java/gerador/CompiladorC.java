package gerador;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Chama o gcc para transformar o código C gerado em executável, e roda o
 * executável para mostrar a saída do programa.
 */
public class CompiladorC {

    /** Tempo máximo de execução do programa gerado (protege contra laço infinito). */
    private static final int LIMITE_EXECUCAO_SEGUNDOS = 10;
    private static final int LIMITE_GCC_SEGUNDOS = 60;

    /** Resultado de um processo: se deu certo, o código de saída e o que foi impresso. */
    public record Resultado(boolean sucesso, int codigoSaida, String saida, boolean tempoEsgotado) {
    }

    private CompiladorC() {
    }

    /** true se o sistema for Windows (o executável ganha a extensão .exe). */
    public static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Caminho do executável correspondente a um arquivo .c. */
    public static Path executavelDe(Path fonteC) {
        String nome = fonteC.getFileName().toString().replaceFirst("\\.c$", "");
        return fonteC.resolveSibling(windows() ? nome + ".exe" : nome);
    }

    /**
     * Compila com: gcc arquivo.c -o arquivo(.exe)
     *
     * @throws IOException se o gcc não for encontrado no sistema
     */
    public static Resultado compilar(Path fonteC, Path executavel) throws IOException {
        List<String> comando = List.of("gcc", fonteC.getFileName().toString(),
                "-o", executavel.getFileName().toString());
        return rodar(comando, fonteC.toAbsolutePath().getParent(), LIMITE_GCC_SEGUNDOS, consoleDoSistema());
    }

    /** Roda o executável gerado e devolve o que ele imprimiu. */
    public static Resultado executar(Path executavel) throws IOException {
        List<String> comando = List.of(executavel.toAbsolutePath().toString());
        // o programa imprime as narrações exatamente como estão no código (UTF-8)
        return rodar(comando, executavel.toAbsolutePath().getParent(), LIMITE_EXECUCAO_SEGUNDOS,
                StandardCharsets.UTF_8);
    }

    private static Resultado rodar(List<String> comando, Path pasta, int limiteSegundos, Charset charset)
            throws IOException {
        File saida = File.createTempFile("compilaco", ".txt");
        try {
            Process processo = new ProcessBuilder(comando)
                    .directory(pasta.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(saida)
                    .redirectInput(ProcessBuilder.Redirect.from(nulo()))
                    .start();

            boolean terminou;
            try {
                terminou = processo.waitFor(limiteSegundos, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                terminou = false;
            }
            if (!terminou) {
                processo.destroyForcibly();
            }

            String texto = new String(Files.readAllBytes(saida.toPath()), charset);
            int codigo = terminou ? processo.exitValue() : -1;
            return new Resultado(terminou && codigo == 0, codigo, texto, !terminou);
        } finally {
            saida.delete();
        }
    }

    private static File nulo() {
        return new File(windows() ? "NUL" : "/dev/null");
    }

    /** Codificação das mensagens do gcc (a do console do sistema). */
    private static Charset consoleDoSistema() {
        String nome = System.getProperty("native.encoding");
        try {
            return nome != null ? Charset.forName(nome) : Charset.defaultCharset();
        } catch (RuntimeException e) {
            return Charset.defaultCharset();
        }
    }
}
