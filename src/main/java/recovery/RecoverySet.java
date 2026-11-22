package recovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import parser.Compilaco;

/**
 * Conjunto de tokens usado na recuperação de erros (modo pânico).
 *
 * Mesma estrutura da classe RecoverySet vista em aula: guarda os tipos
 * (kind) dos tokens e permite unir conjuntos sem alterar os originais.
 */
public class RecoverySet extends HashSet<Integer> {

    public RecoverySet()
    // cria conjunto vazio
    {
        super();
    }

    public RecoverySet(int t)
    // cria conjunto com 1 tipo de token
    {
        this.add(t);
    }

    public boolean contains(int t)
    // verifica se token pertence ao conjunto
    {
        return super.contains(t);
    }

    public RecoverySet union(RecoverySet s)
    // faz a união de dois conjuntos
    {
        RecoverySet t = null;

        if (s != null) // se s == null retorna null
        {
            t = (RecoverySet) this.clone();
            t.addAll(s);
        }

        return t; // retorna um terceiro conjunto, sem destruir nenhum
    }

    public RecoverySet remove(int n) // retira 1 elemento do conjunto
    {
        RecoverySet t = (RecoverySet) this.clone();
        t.remove(Integer.valueOf(n));

        return t; // retorna um novo conjunto, sem 1 dos elementos
    }

    // cria string descrevendo os tokens que pertencem ao conjunto
    // (em ordem fixa, pela ordem dos tokens na gramática)
    @Override
    public String toString() {
        Iterator<Integer> it = new TreeSet<Integer>(this).iterator();
        String s = "";

        while (it.hasNext()) {
            s += (Compilaco.im(it.next()) + " ");
        }

        return s;
    }
}
