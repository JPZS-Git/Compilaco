package gerador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.CompilacoConstants;
import parser.No;
import parser.Token;

/**
 * Geração de código: traduz a árvore sintática do Compilaço para C.
 *
 * Tradução:
 *   apito_inicial() ... fim_de_jogo   ->  int main(void) { ... return 0; }
 *   placar / estatistica / narracao   ->  int / float / char *
 *   var_analisa ... segue_jogo        ->  if ... else
 *   enquanto_bola_rolar               ->  while
 *   campeonato                        ->  for
 *
 * Como o Compilaço não tem comando de saída, o main gerado termina
 * imprimindo o valor final das variáveis declaradas no nível do programa.
 *
 * Só deve ser chamado para um código sem erros (árvore sem nós "erro").
 */
public class GeradorC {

    /** Palavras do C que não podem ser usadas como nome de variável no código gerado. */
    private static final Set<String> RESERVADAS_C = Set.of(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "bool",
            "true", "false", "main", "printf", "NULL");

    private final StringBuilder codigo = new StringBuilder();
    private final Map<String, String> variaveisDoPrograma = new LinkedHashMap<>(); // nome -> tipo C
    private int nivel = 1;

    private GeradorC() {
    }

    /**
     * Gera o código C de um programa Compilaço.
     *
     * @param raiz      árvore sintática devolvida por Compilaco.analisar()
     * @param nomeFonte nome do arquivo de origem (só para o comentário do topo)
     */
    public static String gerar(No raiz, String nomeFonte) {
        if (temErro(raiz)) {
            throw new IllegalArgumentException(
                    "O código tem erros: corrija-os antes de gerar o código C.");
        }
        return new GeradorC().programa(raiz, nomeFonte);
    }

    // ------------------------------------------------------------------
    // Programa e comandos
    // ------------------------------------------------------------------

    private String programa(No raiz, String nomeFonte) {
        codigo.append("/*\n");
        codigo.append(" * Código C gerado pelo compilador Compilaço\n");
        codigo.append(" * a partir de: ").append(nomeFonte).append('\n');
        codigo.append(" */\n");
        codigo.append("#include <stdio.h>\n\n");
        codigo.append("int main(void)\n{\n");

        for (No filho : raiz.getFilhos()) {
            if (filho.getNome().equals("comando")) {
                comando(filho, true);
            }
        }

        if (!variaveisDoPrograma.isEmpty()) {
            codigo.append('\n');
            linha("/* valor final das variáveis (o Compilaço não tem comando de saída) */");
            for (Map.Entry<String, String> v : variaveisDoPrograma.entrySet()) {
                String nome = v.getKey();
                linha("printf(\"" + nome + " = " + formato(v.getValue()) + "\\n\", " + nomeC(nome) + ");");
            }
        }

        codigo.append('\n');
        linha("return 0;");
        codigo.append("}\n");
        return codigo.toString();
    }

    /** comando -> declaraVar | atribuicao | comandoIf | comandoWhile | comandoFor */
    private void comando(No comando, boolean nivelDoPrograma) {
        No c = comando.getFilhos().get(0);
        switch (c.getNome()) {
            case "declaraVar" -> declaracao(c, nivelDoPrograma);
            case "atribuicao" -> atribuicao(c);
            case "comandoIf" -> comandoIf(c);
            case "comandoWhile" -> comandoWhile(c);
            case "comandoFor" -> comandoFor(c);
            default -> throw new IllegalStateException("Comando desconhecido: " + c.getNome());
        }
    }

    /** placar a = 1, b;  ->  int a = 1, b = 0; */
    private void declaracao(No decl, boolean nivelDoPrograma) {
        List<No> f = decl.getFilhos();
        String tipo = tipoC(f.get(0));
        boolean ponteiro = tipo.equals("char *");
        List<String> itens = new ArrayList<>();

        int i = 1;
        while (i < f.size()) {
            Token t = f.get(i).getToken();
            if (t != null && t.kind == CompilacoConstants.IDENTIFICADOR) {
                String valor;
                if (i + 1 < f.size() && tipoDoToken(f.get(i + 1)) == CompilacoConstants.ATRIBUICAO) {
                    valor = expressao(f.get(i + 2));
                    i += 3;
                } else {
                    valor = valorPadrao(tipo); // variável sem valor inicial começa zerada
                    i += 1;
                }
                itens.add((ponteiro ? "*" : "") + nomeC(t.image) + " = " + valor);
                if (nivelDoPrograma) {
                    variaveisDoPrograma.put(t.image, tipo);
                }
            } else {
                i++; // "," ou ";"
            }
        }
        linha((ponteiro ? "char " : tipo + " ") + String.join(", ", itens) + ";");
    }

    /** x = expressao; */
    private void atribuicao(No atrib) {
        List<No> f = atrib.getFilhos();
        linha(nomeC(f.get(0).getToken().image) + " = " + expressao(f.get(2)) + ";");
    }

    /** var_analisa (cond) { } segue_jogo { }  ->  if (cond) { } else { } */
    private void comandoIf(No no) {
        List<No> f = no.getFilhos();
        linha("if (" + expressao(f.get(2)) + ") {");
        bloco(f.get(4));
        if (f.size() > 5) {
            linha("} else {");
            bloco(f.get(6));
        }
        linha("}");
    }

    /** enquanto_bola_rolar (cond) { }  ->  while (cond) { } */
    private void comandoWhile(No no) {
        List<No> f = no.getFilhos();
        linha("while (" + expressao(f.get(2)) + ") {");
        bloco(f.get(4));
        linha("}");
    }

    /** campeonato (placar i = 0; cond; i = expr) { }  ->  for (int i = 0; cond; i = expr) { } */
    private void comandoFor(No no) {
        List<No> f = no.getFilhos();
        String tipo = tipoC(f.get(2));
        boolean ponteiro = tipo.equals("char *");
        String inicio = (ponteiro ? "char *" : tipo + " ") + nomeC(f.get(3).getToken().image)
                + " = " + valor(f.get(5));
        String condicao = expressao(f.get(7));
        String passo = nomeC(f.get(9).getToken().image) + " = " + expressao(f.get(11));
        linha("for (" + inicio + "; " + condicao + "; " + passo + ") {");
        bloco(f.get(13));
        linha("}");
    }

    /** bloco -> { comando* } */
    private void bloco(No bloco) {
        nivel++;
        for (No filho : bloco.getFilhos()) {
            if (filho.getNome().equals("comando")) {
                comando(filho, false);
            }
        }
        nivel--;
    }

    // ------------------------------------------------------------------
    // Expressões (os operadores do Compilaço são os mesmos do C)
    // ------------------------------------------------------------------

    private String expressao(No no) {
        switch (no.getNome()) {
            case "expressao":
            case "exprArit":
            case "termo": {
                StringBuilder sb = new StringBuilder();
                for (No filho : no.getFilhos()) {
                    sb.append(filho.getToken() != null
                            ? " " + filho.getToken().image + " " // operador
                            : expressao(filho));
                }
                return sb.toString();
            }
            case "fator": {
                List<No> f = no.getFilhos();
                if (f.size() == 1) {
                    return valor(f.get(0));                                      // valor
                }
                if (tipoDoToken(f.get(0)) == CompilacoConstants.ABREPAREN) {
                    return "(" + expressao(f.get(1)) + ")";                       // ( expressao )
                }
                return f.get(0).getToken().image + expressao(f.get(1));           // -fator / +fator
            }
            case "valor":
                return valor(no);
            default:
                throw new IllegalStateException("Expressão inesperada: " + no.getNome());
        }
    }

    /** valor -> constante | real | string | identificador */
    private String valor(No valor) {
        Token t = valor.getFilhos().get(0).getToken();
        switch (t.kind) {
            case CompilacoConstants.STRING_LIT:
                return stringC(t.image);
            case CompilacoConstants.IDENTIFICADOR:
                return nomeC(t.image);
            default:
                return t.image; // números são escritos igual em C
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void linha(String texto) {
        codigo.append("    ".repeat(nivel)).append(texto).append('\n');
    }

    private static String tipoC(No tipoDado) {
        int tipo = tipoDado.getFilhos().get(0).getToken().kind;
        return switch (tipo) {
            case CompilacoConstants.TIPOINT -> "int";        // placar
            case CompilacoConstants.TIPOFLOAT -> "float";    // estatistica
            default -> "char *";                             // narracao
        };
    }

    private static String valorPadrao(String tipo) {
        return switch (tipo) {
            case "int" -> "0";
            case "float" -> "0.0f";
            default -> "\"\"";
        };
    }

    private static String formato(String tipo) {
        return switch (tipo) {
            case "int" -> "%d";
            case "float" -> "%g";
            default -> "%s";
        };
    }

    /** Nome da variável no C: se coincidir com uma palavra do C, ganha o prefixo "v_". */
    private static String nomeC(String nome) {
        return RESERVADAS_C.contains(nome) ? "v_" + nome : nome;
    }

    /** Converte a narração do Compilaço numa string C válida. */
    private static String stringC(String imagem) {
        String conteudo = imagem.substring(1, imagem.length() - 1);
        StringBuilder sb = new StringBuilder("\"");
        for (char c : conteudo.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static int tipoDoToken(No no) {
        return no.getToken() != null ? no.getToken().kind : -1;
    }

    private static boolean temErro(No no) {
        if (no.isErro()) {
            return true;
        }
        for (No filho : no.getFilhos()) {
            if (temErro(filho)) {
                return true;
            }
        }
        return false;
    }
}
