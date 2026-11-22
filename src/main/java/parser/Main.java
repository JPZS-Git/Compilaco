package parser;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Execução pelo terminal:
 *
 *   java -cp out parser.Main [arquivo] [--arvore] [--debug]
 *
 * Sem o caminho do arquivo, abre um JFileChooser para escolher o código.
 * Com "-" no lugar do arquivo, lê o código da entrada padrão (útil para testes
 * rápidos no terminal, sem criar arquivo).
 * O arquivo é lido inteiro em UTF-8 e passado ao parser (sem cortar em fim_de_jogo).
 */
public class Main {

    public static void main(String[] args) {
        boolean debug = false;
        boolean mostrarArvore = false;
        boolean entradaPadrao = false;
        Path caminho = null;

        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) {
                debug = true;
            } else if ("--arvore".equalsIgnoreCase(arg)) {
                mostrarArvore = true;
            } else if ("-".equals(arg)) {
                entradaPadrao = true;
            } else if (caminho == null) {
                caminho = Path.of(arg);
            }
        }

        System.out.println("\n[Juiz em campo] Apito inicial!");

        ArquivoFonte arquivo = new ArquivoFonte();
        String codigo;

        if (entradaPadrao) {
            try {
                codigo = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                imprimirErros(List.of(new ErroCompilacao(
                        TipoErro.ENTRADA, 0, 0,
                        "Não foi possível ler a entrada: " + e.getMessage()
                )));
                return;
            }
            if (!codigo.isEmpty() && codigo.charAt(0) == '\uFEFF') {
                codigo = codigo.substring(1); // BOM
            }
            System.out.println("Código lido da entrada padrão.\n");
            executar(codigo, debug, mostrarArvore);
            return;
        }

        try {
            if (caminho != null) {
                codigo = arquivo.abrir(caminho);
            } else {
                System.out.println("Escolha o arquivo com o código Compilaco...");
                codigo = arquivo.abrir((java.awt.Component) null);
                if (codigo == null) {
                    System.out.println("Nenhum arquivo selecionado. Fim de jogo antes do apito!");
                    return;
                }
            }
        } catch (IOException e) {
            imprimirErros(List.of(new ErroCompilacao(
                    TipoErro.ENTRADA, 0, 0,
                    "Não foi possível ler o arquivo: " + e.getMessage()
            )));
            return;
        }

        System.out.println("Arquivo: " + arquivo.getCaminho().toAbsolutePath() + "\n");
        executar(codigo, debug, mostrarArvore);
    }

    /** Analisa o código inteiro (o conteúdo completo do arquivo). */
    private static void executar(String codigo, boolean debug, boolean mostrarArvore) {
        if (debug) {
            System.out.println("[DEBUG] Código recebido:");
            System.out.println("----------------------------------------");
            System.out.print(codigo);
            System.out.println("----------------------------------------");
            imprimirTokens(codigo);
            System.out.println("[DEBUG] Iniciando análise sintática...\n");
        }

        List<ErroCompilacao> erros;
        No arvore;

        try {
            // O texto vai como String (StringReader): assim o parser não depende
            // do charset padrão do sistema e os acentos chegam intactos.
            Compilaco parser = Compilaco.criar(new StringReader(codigo));
            arvore = parser.analisar();
            erros = new ArrayList<>(parser.erros);

            if (debug && !parser.rastro.isEmpty()) {
                System.out.println("[DEBUG] Recuperação de erros (modo pânico):");
                parser.rastro.forEach(System.out::println);
                System.out.println();
            }
        } catch (RuntimeException e) {
            imprimirErros(List.of(new ErroCompilacao(
                    TipoErro.INTERNO, 0, 0,
                    "Erro interno do compilador: " + e.getMessage()
            )));
            return;
        }

        // erros léxicos e sintáticos na ordem em que aparecem no código
        erros.sort(Comparator.comparingInt(ErroCompilacao::linha)
                .thenComparingInt(ErroCompilacao::coluna));

        if (erros.isEmpty()) {
            System.out.println("\n[GOL] Análise léxica e sintática concluída sem erros!");
            System.out.println("=> Fim de jogo! O código é sintaticamente válido.");
        } else {
            imprimirErros(erros);
        }

        if (debug || mostrarArvore) {
            System.out.println("\n[ARVORE SINTATICA]");
            System.out.print(arvore.imprimir());
        }
    }

    /** Lista os tokens (com um léxico separado, só para exibição). */
    private static void imprimirTokens(String codigo) {
        System.out.println("[DEBUG] Tokens reconhecidos pelo lexer:");

        List<ErroCompilacao> ignorados = new ArrayList<>();
        LexicoComRecuperacao lexico = new LexicoComRecuperacao(
                new SimpleCharStream(new StringReader(codigo)), ignorados);

        while (true) {
            Token token = lexico.getNextToken();

            String nome = token.kind >= 0 &&
                    token.kind < CompilacoConstants.tokenImage.length
                    ? CompilacoConstants.tokenImage[token.kind]
                    : String.valueOf(token.kind);

            String imagem = token.kind == CompilacoConstants.EOF
                    ? "<EOF>"
                    : "'" + token.image + "'";

            System.out.printf(
                    "  linha %-3d coluna %-3d %-22s -> %s%n",
                    token.beginLine,
                    token.beginColumn,
                    nome,
                    imagem
            );

            if (token.kind == CompilacoConstants.EOF) {
                break;
            }
        }

        System.out.println();
    }

    private static void imprimirErros(List<ErroCompilacao> erros) {
        if (erros.isEmpty()) {
            return;
        }

        System.out.println("\n[FALTA GRAVE] O VAR encontrou " + erros.size()
                + (erros.size() == 1 ? " erro:" : " erros:"));

        for (ErroCompilacao erro : erros) {
            String posicao = erro.linha() > 0
                    ? "linha " + erro.linha() + ", coluna " + erro.coluna()
                    : "posição desconhecida";

            System.out.printf(
                    "[%s] %s: %s%n",
                    erro.tipo(),
                    posicao,
                    erro.mensagem()
            );
        }
    }
}