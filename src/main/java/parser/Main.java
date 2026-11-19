package parser;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Execução pelo terminal:
 *
 *   java -cp out parser.Main [arquivo] [--arvore] [--debug]
 *
 * Sem o caminho do arquivo, abre um JFileChooser para escolher o código.
 * O arquivo é lido inteiro em UTF-8 e passado ao parser (sem cortar em fim_de_jogo).
 */
public class Main {

    private static final Pattern POSICAO_ERRO =
            Pattern.compile("linha\\s+(\\d+),\\s*coluna\\s+(\\d+)");

    public static void main(String[] args) {
        boolean debug = false;
        boolean mostrarArvore = false;
        Path caminho = null;

        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) {
                debug = true;
            } else if ("--arvore".equalsIgnoreCase(arg)) {
                mostrarArvore = true;
            } else if (caminho == null) {
                caminho = Path.of(arg);
            }
        }

        System.out.println("\n[Juiz em campo] Apito inicial!");

        ArquivoFonte arquivo = new ArquivoFonte();
        String codigo;

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
        }

        List<ErroCompilacao> erros = new ArrayList<>();

        try {
            // O texto vai como String (StringReader): assim o parser não depende
            // do charset padrão do sistema e os acentos chegam intactos.
            Compilaco parser = new Compilaco(new StringReader(codigo));

            if (debug) {
                imprimirTokens(parser);
                parser.ReInit(new StringReader(codigo));
                System.out.println("[DEBUG] Iniciando análise sintática...\n");
            }

            No arvore = parser.inicio();

            System.out.println("\n[GOL] Análise léxica e sintática concluída sem erros!");
            System.out.println("=> Fim de jogo! O código é sintaticamente válido.");

            if (debug || mostrarArvore) {
                System.out.println("\n[ARVORE SINTATICA]");
                System.out.print(arvore.imprimir());
            }

        } catch (TokenMgrError e) {
            erros.add(converterErroLexico(e));
        } catch (ParseException e) {
            erros.add(converterErroSintatico(e));
        } catch (RuntimeException e) {
            erros.add(new ErroCompilacao(
                    TipoErro.INTERNO,
                    0,
                    0,
                    "Erro interno do compilador: " + e.getMessage()
            ));
        }

        imprimirErros(erros);
    }

    private static void imprimirTokens(Compilaco parser) {
        System.out.println("[DEBUG] Tokens reconhecidos pelo lexer:");

        while (true) {
            Token token = parser.token_source.getNextToken();

            String nome = token.kind >= 0 &&
                    token.kind < Compilaco.tokenImage.length
                    ? Compilaco.tokenImage[token.kind]
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

    private static ErroCompilacao converterErroLexico(TokenMgrError erro) {
        int linha = 0;
        int coluna = 0;

        Matcher matcher = POSICAO_ERRO.matcher(erro.getMessage());

        if (matcher.find()) {
            linha = Integer.parseInt(matcher.group(1));
            coluna = Integer.parseInt(matcher.group(2));
        }

        return new ErroCompilacao(
                TipoErro.LEXICO,
                linha,
                coluna,
                erro.getMessage()
        );
    }

    /* ESSA VERSÃO DEVE SER APLICADA DEPOIS DE SER FEITA AS MODIFICAÇÕES NO JAVACC
    private static ErroCompilacao converterErroLexico(TokenMgrError erro) {
        return new ErroCompilacao(
                TipoErro.LEXICO,
                erro.getErrorLine(),
                erro.getErrorColumn(),
                erro.getMessage()
        );
    }
     */

    private static ErroCompilacao converterErroSintatico(ParseException erro) {
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

        for (int[] sequencia : erro.expectedTokenSequences) {
            if (sequencia.length > 0) {
                esperados.add(Compilaco.tokenImage[sequencia[0]]);
            }
        }

        String mensagem = "Token inesperado: " + encontrado + ".";

        if (!esperados.isEmpty()) {
            mensagem += " Esperado: " + String.join(", ", esperados) + ".";
        }

        return new ErroCompilacao(
                TipoErro.SINTATICO,
                linha,
                coluna,
                mensagem
        );
    }

    private static void imprimirErros(List<ErroCompilacao> erros) {
        if (erros.isEmpty()) {
            return;
        }

        System.out.println("\n[VAR - RELATÓRIO DE ERROS]");

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