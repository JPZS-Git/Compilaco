package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nó da árvore sintática.
 *
 * Cada regra da gramática (inicio, comando, expressao, termo...) gera um nó
 * com o nome da regra; cada token consumido vira uma folha com o seu lexema.
 *
 * Exemplo para "2 + 3 * 4":
 *
 *   expressao
 *   `-- exprArit
 *       |-- termo
 *       |   `-- fator
 *       |       `-- valor
 *       |           `-- 2
 *       |-- +
 *       `-- termo
 *           |-- fator ... 3
 *           |-- *
 *           `-- fator ... 4
 */
public class No {

    private final String nome;
    private final List<No> filhos = new ArrayList<>();

    /** Token de origem (só nas folhas); usado para saber linha e coluna. */
    private final Token token;

    public No(String nome) {
        this(nome, null);
    }

    private No(String nome, Token token) {
        this.nome = nome;
        this.token = token;
    }

    /** Folha criada a partir de um token reconhecido pelo léxico. */
    public static No folha(Token token) {
        return new No(token.image, token);
    }

    /** Nó colocado no lugar de um comando descartado pelo modo pânico. */
    public static No erro(String mensagem) {
        return new No(mensagem == null || mensagem.isEmpty() ? "erro" : "erro: " + mensagem);
    }

    /** Adiciona um filho (ignora null) e devolve o próprio nó, para encadear. */
    public No add(No filho) {
        if (filho != null) {
            filhos.add(filho);
        }
        return this;
    }

    /** Atalho para adicionar um token como folha. */
    public No add(Token token) {
        if (token != null) {
            filhos.add(folha(token));
        }
        return this;
    }

    public String getNome() {
        return nome;
    }

    public List<No> getFilhos() {
        return Collections.unmodifiableList(filhos);
    }

    public Token getToken() {
        return token;
    }

    public boolean isFolha() {
        return filhos.isEmpty();
    }

    public boolean isErro() {
        return nome.startsWith("erro");
    }

    /** Usado pelo JTree (DefaultMutableTreeNode) para mostrar o texto do nó. */
    @Override
    public String toString() {
        return nome;
    }

    /** Árvore em texto, com indentação (só caracteres ASCII, para o console do Windows). */
    public String imprimir() {
        StringBuilder sb = new StringBuilder();
        sb.append(nome).append('\n');
        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimir(sb, "", i == filhos.size() - 1);
        }
        return sb.toString();
    }

    private void imprimir(StringBuilder sb, String prefixo, boolean ultimo) {
        sb.append(prefixo).append(ultimo ? "`-- " : "|-- ").append(nome).append('\n');
        String prefixoFilhos = prefixo + (ultimo ? "    " : "|   ");
        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimir(sb, prefixoFilhos, i == filhos.size() - 1);
        }
    }
}
