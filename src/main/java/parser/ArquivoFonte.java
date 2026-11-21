package parser;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Leitura e gravação dos arquivos de código-fonte Compilaco, sempre em UTF-8.
 *
 * Uso típico na interface gráfica (um objeto por editor):
 *
 *   ArquivoFonte arquivo = new ArquivoFonte();
 *
 *   // botão Abrir
 *   String codigo = arquivo.abrir(janela);         // null se cancelar
 *   if (codigo != null) editor.setText(codigo);
 *
 *   // botão Salvar
 *   arquivo.salvar(janela, editor.getText());      // pede o nome se for arquivo novo
 *
 *   // botão Compilar
 *   new Compilaco(new java.io.StringReader(editor.getText()));
 *
 * O texto é passado inteiro ao parser, sem cortar em fim_de_jogo.
 */
public class ArquivoFonte {

    private static final String EXTENSAO = "txt";
    private static final char BOM = '﻿';

    /** Arquivo aberto/salvo por último; null quando o código ainda não foi salvo. */
    private Path caminho;

    /** Pasta onde o seletor abre; começa na pasta de exemplos, se existir. */
    private File ultimaPasta = new File("examples").isDirectory()
            ? new File("examples").getAbsoluteFile()
            : new File(".").getAbsoluteFile();

    // ------------------------------------------------------------------
    // Leitura e gravação (sem interface)
    // ------------------------------------------------------------------

    /**
     * Lê o arquivo inteiro em UTF-8.
     * Remove o BOM do início (o Bloco de Notas às vezes grava um), que
     * senão viraria um símbolo inválido na linha 1, coluna 1.
     */
    public static String ler(Path caminho) throws IOException {
        try {
            String texto = Files.readString(caminho, StandardCharsets.UTF_8);
            if (!texto.isEmpty() && texto.charAt(0) == BOM) {
                texto = texto.substring(1);
            }
            return texto;
        } catch (NoSuchFileException e) {
            throw new IOException("Arquivo não encontrado: " + caminho, e);
        } catch (CharacterCodingException e) {
            throw new IOException(
                    "O arquivo não está em UTF-8: " + caminho
                    + ". Salve-o novamente com a codificação UTF-8.", e);
        }
    }

    /** Grava o texto inteiro em UTF-8 (sem BOM), substituindo o conteúdo anterior. */
    public static void gravar(Path caminho, String texto) throws IOException {
        Files.writeString(caminho, texto, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // Operações com JFileChooser (para a interface e para o Main)
    // ------------------------------------------------------------------

    /**
     * Mostra o seletor de arquivos e lê o arquivo escolhido.
     *
     * @param pai janela dona do diálogo (pode ser null)
     * @return o texto do arquivo, ou null se o usuário cancelar
     */
    public String abrir(Component pai) throws IOException {
        Path escolhido = escolherParaAbrir(pai);
        if (escolhido == null) {
            return null;
        }
        String texto = ler(escolhido);
        caminho = escolhido;
        return texto;
    }

    /** Abre um caminho já conhecido (ex.: passado por argumento), sem diálogo. */
    public String abrir(Path arquivo) throws IOException {
        String texto = ler(arquivo);
        caminho = arquivo;
        ultimaPasta = pastaDe(arquivo);
        return texto;
    }

    /**
     * Salva no arquivo atual; se o código ainda não tem arquivo, pergunta onde salvar.
     *
     * @return true se salvou, false se o usuário cancelou
     */
    public boolean salvar(Component pai, String texto) throws IOException {
        if (caminho == null) {
            return salvarComo(pai, texto);
        }
        gravar(caminho, texto);
        return true;
    }

    /**
     * Pergunta onde salvar e grava o texto lá (confirma antes de sobrescrever).
     *
     * @return true se salvou, false se o usuário cancelou
     */
    public boolean salvarComo(Component pai, String texto) throws IOException {
        Path destino = escolherParaSalvar(pai);
        if (destino == null) {
            return false;
        }
        gravar(destino, texto);
        caminho = destino;
        return true;
    }

    /** Mostra o seletor "Abrir" e devolve o arquivo escolhido (ou null). */
    public Path escolherParaAbrir(Component pai) {
        JFileChooser seletor = criarSeletor();
        seletor.setDialogTitle("Abrir código Compilaco");

        if (seletor.showOpenDialog(pai) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        ultimaPasta = seletor.getCurrentDirectory();
        return seletor.getSelectedFile().toPath();
    }

    /** Mostra o seletor "Salvar" e devolve o destino escolhido (ou null). */
    public Path escolherParaSalvar(Component pai) {
        JFileChooser seletor = criarSeletor();
        seletor.setDialogTitle("Salvar código Compilaco");
        if (caminho != null) {
            seletor.setSelectedFile(caminho.toFile());
        }

        while (true) {
            if (seletor.showSaveDialog(pai) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            ultimaPasta = seletor.getCurrentDirectory();
            Path destino = comExtensao(seletor.getSelectedFile().toPath());

            if (!Files.exists(destino) || confirmarSobrescrever(pai, destino)) {
                return destino;
            }
            // usuário não quis sobrescrever: mostra o seletor de novo
        }
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    /** Arquivo atual (null se o código ainda não foi salvo). */
    public Path getCaminho() {
        return caminho;
    }

    /** Nome do arquivo atual, para título de janela/aba. */
    public String getNome() {
        return caminho == null ? "sem título" : caminho.getFileName().toString();
    }

    /** Começa um código novo (o próximo salvar vai perguntar o nome). */
    public void novo() {
        caminho = null;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private JFileChooser criarSeletor() {
        JFileChooser seletor = new JFileChooser(ultimaPasta);
        seletor.setFileFilter(new FileNameExtensionFilter(
                "Código Compilaco (*." + EXTENSAO + ")", EXTENSAO));
        return seletor;
    }

    /** Se o nome digitado não tem extensão, acrescenta .txt. */
    private static Path comExtensao(Path destino) {
        String nome = destino.getFileName().toString();
        if (nome.contains(".")) {
            return destino;
        }
        return destino.resolveSibling(nome + "." + EXTENSAO);
    }

    private static boolean confirmarSobrescrever(Component pai, Path destino) {
        int resposta = JOptionPane.showConfirmDialog(
                pai,
                "O arquivo " + destino.getFileName() + " já existe. Deseja substituí-lo?",
                "Confirmar substituição",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return resposta == JOptionPane.YES_OPTION;
    }

    private static File pastaDe(Path arquivo) {
        Path pasta = arquivo.toAbsolutePath().getParent();
        return pasta != null ? pasta.toFile() : new File(".").getAbsoluteFile();
    }
}
