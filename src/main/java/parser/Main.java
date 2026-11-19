package parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final String TERMINADOR = "fim_de_jogo";
    private static final Pattern POSICAO_ERRO =
            Pattern.compile("linha\\s+(\\d+),\\s*coluna\\s+(\\d+)");

    public static void main(String[] args) {
        boolean debug = List.of(args).contains("--debug");
        boolean mostrarArvore = List.of(args).contains("--arvore");

        System.out.println("\n[Juiz em campo] Apito inicial!");
        System.out.println("Digite o código. A análise começa ao encontrar 'fim_de_jogo'.\n");

        try {
            executar(debug, mostrarArvore);
        } catch (IOException e) {
            List<ErroCompilacao> erros = new ArrayList<>();
            erros.add(new ErroCompilacao(
                    TipoErro.ENTRADA, 0, 0,
                    "Não foi possível ler a entrada: " + e.getMessage()
            ));
            imprimirErros(erros);
        }
    }

    private static void executar(boolean debug, boolean mostrarArvore) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );

        StringBuilder codigo = new StringBuilder();
        String linha;
        boolean encontrouFim = false;
        int numeroLinha = 0;

        while ((linha = reader.readLine()) != null) {
            numeroLinha++;
            codigo.append(linha).append('\n');

            if (linha.trim().equals(TERMINADOR)) {
                encontrouFim = true;
                break;
            }
        }

        if (!encontrouFim) {
            imprimirErros(List.of(new ErroCompilacao(
                    TipoErro.ENTRADA,
                    numeroLinha,
                    1,
                    "O código terminou sem o terminador 'fim_de_jogo'."
            )));
            return;
        }

        if (debug) {
            System.out.println("[DEBUG] Código recebido:");
            System.out.println("----------------------------------------");
            System.out.print(codigo);
            System.out.println("----------------------------------------");
        }

        byte[] codigoBytes = codigo.toString().getBytes(StandardCharsets.UTF_8);
        List<ErroCompilacao> erros = new ArrayList<>();

        try {
            // STATIC = false: o parser agora é um objeto (não há mais métodos estáticos)
            Compilaco parser = new Compilaco(new ByteArrayInputStream(codigoBytes));

            if (debug) {
                imprimirTokens(parser);
                parser.ReInit(new ByteArrayInputStream(codigoBytes));
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