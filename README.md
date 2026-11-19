# Compilaco

Alunos:
 - Bolivar Torres Neto
 - Guilherme Campos Feuser
 - Matheus Lordron
 - João Pedro Zanette

Compilaco é uma linguagem feita utilizando JavaCC com o tema de futebol.

## Estrutura

```text
Compilaco-generico/
├── src/
│   └── main/
│       ├── java/
│       │   └── parser/
│       │       ├── ArquivoFonte.java    (leitura/gravação UTF-8 + JFileChooser)
│       │       ├── Compilaco.java
│       │       ├── CompilacoConstants.java
│       │       ├── CompilacoTokenManager.java
│       │       ├── Main.java
│       │       ├── No.java              (nó da árvore sintática)
│       │       ├── ParseException.java
│       │       ├── SimpleCharStream.java
│       │       ├── Token.java
│       │       └── TokenMgrError.java
│       └── javacc/
│           └── parser/
│               └── Compilaco.jj
├── examples/
│   ├── codigo_valido.txt
│   └── codigo_com_erro.txt
├── pom.xml
├── build.bat / build.sh
└── run-valid.bat / run-valid.sh
```

## Abrindo em uma IDE

Abra a pasta raiz do projeto. Se a IDE reconhecer Maven, importe o `pom.xml`.

A classe principal é:

```text
parser.Main
```

O diretório de fontes Java é:

```text
src/main/java
```

A gramática JavaCC está em:

```text
src/main/javacc/parser/Compilaco.jj
```

Os arquivos Java gerados pelo JavaCC já estão incluídos, portanto o projeto pode ser compilado imediatamente. Se alterar `Compilaco.jj`, regenere os arquivos Java usando JavaCC.

## Teste rápido no Windows

Abra um terminal na raiz e execute:

```bat
.\run-valid.bat
```

Para testar o erro sintático:

```bat
.\run-error.bat
```

## Teste rápido no Linux/macOS

```sh
./run-valid.sh
```

ou:

```sh
./run-error.sh
```

## Executando manualmente

Compile:

```text
javac -encoding UTF-8 -d out src/main/java/parser/*.java
```

Execute passando o arquivo com o código:

```text
java -cp out parser.Main examples/codigo_valido.txt --debug
```

Sem o caminho do arquivo, abre uma janela (JFileChooser) para escolher o código:

```text
java -cp out parser.Main --debug
```

Opções (em qualquer ordem):

| Opção | Efeito |
|---|---|
| `arquivo` | Caminho do código a analisar. Sem ele, abre o seletor de arquivos. |
| `--arvore` | Imprime a árvore sintática depois de uma análise sem erros. |
| `--debug` | Mostra o código lido, os tokens e a árvore. |

## Leitura e gravação de arquivos

A classe `ArquivoFonte` cuida dos arquivos de código:

- lê o arquivo **inteiro** com `Files.readString` e o parser recebe todo o texto, sem cortar em `fim_de_jogo` (se houver algo depois dele, é apontado como erro sintático);
- lê e grava sempre em **UTF-8** (o BOM que o Bloco de Notas às vezes grava é ignorado; arquivo que não está em UTF-8 gera um erro de entrada com aviso);
- `abrir(janela)` mostra o `JFileChooser` e devolve o texto (ou `null` se cancelar);
- `salvar(janela, texto)` grava no arquivo atual ou, se o código for novo, pergunta onde salvar; `salvarComo(janela, texto)` sempre pergunta. Ao salvar, pede confirmação antes de substituir um arquivo e acrescenta `.txt` se o nome não tiver extensão.

Na interface gráfica, os botões usam um único objeto:

```java
ArquivoFonte arquivo = new ArquivoFonte();

String codigo = arquivo.abrir(janela);          // botão Abrir
if (codigo != null) editor.setText(codigo);

arquivo.salvar(janela, editor.getText());       // botão Salvar

new Compilaco(new java.io.StringReader(editor.getText())); // botão Compilar
```

O parser recebe o texto por `StringReader` (e não por bytes), assim os acentos dentro de `narracao` chegam corretos, independentemente do charset padrão do sistema.

## Árvore sintática

Cada regra da gramática (`inicio`, `comando`, `expressao`, `termo`...) devolve um nó da classe `No`, com o nome da regra; os filhos são as sub-regras e os tokens consumidos, na ordem em que aparecem. A árvore é impressa no terminal depois de uma análise sem erros, com a opção `--arvore` (ou `--debug`, que também mostra os tokens).

### Windows (PowerShell)

Na raiz do projeto, compile:

```powershell
.\build.bat
```

Árvore do exemplo válido:

```powershell
java -cp out parser.Main examples\codigo_valido.txt --arvore
```

Exemplo com erro (mostra o relatório de erros, sem árvore):

```powershell
java -cp out parser.Main examples\codigo_com_erro.txt --arvore
```

Escolhendo o arquivo numa janela (JFileChooser):

```powershell
java -cp out parser.Main --arvore
```

### Linux/macOS

```sh
./build.sh
java -cp out parser.Main examples/codigo_valido.txt --arvore
```

### IntelliJ

Em **Run → Edit Configurations…**, crie uma configuração *Application* com:

- **Main class:** `parser.Main`
- **Program arguments:** `examples\codigo_valido.txt --arvore` (sem o arquivo, abre o seletor de arquivos)
- **Working directory:** a pasta raiz do projeto

### Exemplo: precedência em `2 + 3 * 4`

Entrada:

```text
apito_inicial()
placar x = 2 + 3 * 4;
fim_de_jogo
```

Saída (o `*` fica dentro de um `termo`, abaixo do `+`):

```text
inicio
|-- apito_inicial
|-- (
|-- )
|-- comando
|   `-- declaraVar
|       |-- tipoDado
|       |   `-- placar
|       |-- x
|       |-- =
|       |-- expressao
|       |   `-- exprArit
|       |       |-- termo
|       |       |   `-- fator
|       |       |       `-- valor
|       |       |           `-- 2
|       |       |-- +
|       |       `-- termo
|       |           |-- fator
|       |           |   `-- valor
|       |           |       `-- 3
|       |           |-- *
|       |           `-- fator
|       |               `-- valor
|       |                   `-- 4
|       `-- ;
`-- fim_de_jogo
```

### Acentos no terminal do Windows

Se aparecer `?` ou caracteres estranhos no lugar dos acentos (ex.: `RELAT?RIO`, `c├│digo`), é só a página de código do console. Antes de rodar, use:

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

ou, se não resolver:

```powershell
chcp 850
java "-Dsun.stdout.encoding=cp850" -cp out parser.Main --arvore
```

### Regenerando o parser

Se alterar `Compilaco.jj`, regenere os arquivos Java com JavaCC 7 informando a codificação UTF-8 (sem isso, no Windows os acentos da gramática quebram a compilação):

```text
java -Dfile.encoding=UTF-8 -cp javacc.jar javacc -GRAMMAR_ENCODING=UTF-8 -OUTPUT_DIRECTORY=src/main/java/parser src/main/javacc/parser/Compilaco.jj
```

## Observação

Neste estágio o projeto realiza análise léxica e sintática. Ele ainda não interpreta/executa as instruções da linguagem nem possui análise semântica completa.
