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
│       │   ├── parser/
│       │   │   ├── ArquivoFonte.java          (leitura/gravação UTF-8 + JFileChooser)
│       │   │   ├── Compilaco.java
│       │   │   ├── CompilacoConstants.java
│       │   │   ├── CompilacoTokenManager.java
│       │   │   ├── ErroCompilacao.java        (erro: tipo, linha, coluna, mensagem)
│       │   │   ├── LexicoComRecuperacao.java  (léxico que continua após símbolo inválido)
│       │   │   ├── Main.java
│       │   │   ├── No.java                    (nó da árvore sintática)
│       │   │   ├── ParseException.java
│       │   │   ├── SimpleCharStream.java
│       │   │   ├── TipoErro.java
│       │   │   ├── Token.java
│       │   │   └── TokenMgrError.java
│       │   ├── gerador/                       (geração de código C + gcc)
│       │   │   ├── GeradorC.java              (traduz a árvore sintática para C)
│       │   │   └── CompiladorC.java           (chama o gcc e roda o executável)
│       │   ├── gui/                           (interface gráfica)
│       │   │   ├── InterfaceGrafica.java      (janela principal)
│       │   │   ├── NumeracaoLinhas.java       (numeração das linhas do editor)
│       │   │   └── RealceErros.java           (realce das linhas com erro)
│       │   └── recovery/                      (modo pânico)
│       │       ├── RecoverySet.java
│       │       ├── First.java
│       │       ├── Follow.java
│       │       └── ParseEOFException.java
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

## Interface gráfica

No Windows:

```bat
.\run-gui.bat
```

No Linux/macOS:

```sh
./run-gui.sh
```

Ou, depois de compilar com `build.bat`/`build.sh`:

```text
java -cp out gui.InterfaceGrafica [arquivo]
```

A janela tem:

- **Editor** com o código sendo elaborado, numeração das linhas e, na barra de baixo, a contagem de linhas e a posição do cursor;
- **Botões Novo, Abrir, Salvar, Salvar como, Compilar e Gerar executável** (atalhos Ctrl+N, Ctrl+O, Ctrl+S, Ctrl+Shift+S, F5 e F6);
- **Aba Tokens**: cada token reconhecido, com lexema, tipo (como na gramática), linha e coluna;
- **Aba Erros**: todos os erros da compilação (léxicos e sintáticos), com linha, coluna e mensagem; clicar num erro mostra a mensagem completa e leva o cursor até ele;
- **Aba Árvore sintática**: a árvore em `JTree`, com os nós de erro em vermelho;
- **Aba Modo pânico**: o passo a passo da recuperação de cada erro;
- **Resultado**: "GOL!" (verde) quando o código é aceito, ou "FALTA GRAVE" (vermelho) com o total de erros;
- **Realce das linhas com erro**: a linha fica com fundo vermelho (também na numeração), o trecho do erro ganha um sublinhado ondulado, e parar o mouse sobre a linha mostra a mensagem;
- **Aba Código C**: o programa traduzido para C, a compilação com gcc e a saída do executável (ver abaixo).

A interface não faz análise nenhuma: ela chama o compilador (`Compilaco.criar(...).analisar()`) e mostra o resultado.

## Geração de código executável (C + gcc)

O botão **Gerar executável (F6)** da interface:

1. compila o código Compilaço (só continua se não houver erros);
2. percorre a árvore sintática e escreve o programa equivalente em C (`gerador/GeradorC.java`);
3. salva o `.c` numa pasta `saida` ao lado do código-fonte (ignorada pelo Git);
4. chama `gcc arquivo.c -o arquivo` (`gerador/CompiladorC.java`);
. roda o executável e mostra o que ele imprimiu (com limite de 10 segundos, contra laço infinito).

| Compilaço | C |
|---|---|
| `apito_inicial()` … `fim_de_jogo` | `int main(void) { … return 0; }` |
| `placar` / `estatistica` / `narracao` | `int` / `float` / `char *` |
| `var_analisa (…) { } segue_jogo { }` | `if (…) { } else { }` |
| `enquanto_bola_rolar (…) { }` | `while (…) { }` |
| `campeonato (…) { }` | `for (…) { }` |

Como o Compilaço não tem comando de saída, o `main` gerado termina imprimindo o valor final das variáveis declaradas no nível do programa. Variáveis sem valor inicial começam zeradas, e nomes que coincidem com palavras do C (ex.: `main`, `int`) ganham o prefixo `v_`.

Exemplo: `examples/codigo_executavel.txt` gera um executável que imprime:

```text
gols = 3
adversario = 2
posse = 63
resultado = vitória do time da casa
pontos = 18
saldo = 5
```

**gcc no Windows:** instale o MinGW-w64 (por exemplo, pelo MSYS2 ou pelo pacote do WinLibs) e coloque a pasta `bin` dele no PATH. Sem o gcc, a interface salva o `.c` e avisa que o executável não pôde ser gerado.

**Limitação:** o Compilaço ainda não tem análise semântica, então um código sintaticamente válido pode gerar um C que o gcc recusa. Por exemplo, em `examples/codigo_valido.txt` a variável `saldo` é usada sem ser declarada: o gcc aponta o erro e a interface mostra a mensagem dele.

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
javac -encoding UTF-8 -d out src/main/java/parser/*.java src/main/java/recovery/*.java src/main/java/gui/*.java src/main/java/gerador/*.java
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
| `-` | Lê o código da entrada padrão, sem arquivo (ex.: colar o código no terminal com `@'...'@ \| java -cp out parser.Main -`). |
| `--arvore` | Imprime a árvore sintática (com nós `erro` onde houve recuperação). |
| `--debug` | Mostra o código lido, os tokens, o passo a passo do modo pânico e a árvore. |

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

Compilaco parser = Compilaco.criar(new java.io.StringReader(editor.getText())); // botão Compilar
No arvore = parser.analisar();      // árvore (com nós "erro" onde houve recuperação)
List<ErroCompilacao> erros = parser.erros; // todos os erros, léxicos e sintáticos
```

O parser recebe o texto por `StringReader` (e não por bytes), assim os acentos dentro de `narracao` chegam corretos, independentemente do charset padrão do sistema.

## Árvore sintática

Cada regra da gramática (`inicio`, `comando`, `expressao`, `termo`...) devolve um nó da classe `No`, com o nome da regra; os filhos são as sub-regras e os tokens consumidos, na ordem em que aparecem. A árvore é impressa no terminal com a opção `--arvore` (ou `--debug`, que também mostra os tokens). Quando há erros, a árvore continua sendo montada: o trecho descartado pelo modo pânico aparece como um nó `erro`.

### Windows (PowerShell)

Na raiz do projeto, compile:

```powershell
.\build.bat
```

Árvore do exemplo válido:

```powershell
java -cp out parser.Main examples\codigo_valido.txt --arvore
```

Exemplo com erro (mostra o relatório de erros e a árvore com o nó `erro`):

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

## Modo pânico (recuperação de erros)

O compilador não para no primeiro erro: ele registra o erro, descarta tokens até um ponto seguro e continua, mostrando **todos os erros numa única compilação**. A implementação segue o código de recuperação visto em aula (`RecoverySet`, conjuntos `First`, `consumeUntil` e `ParseEOFException`).

**Sintático.** As regras `inicio`, `comando` e as condições de `var_analisa`, `enquanto_bola_rolar` e `campeonato` envolvem sua parte em `try/catch`. No `catch`, `consumeUntil(g, e, "regra")`:

1. registra o erro (com linha, coluna, token encontrado e tokens esperados);
2. descarta tokens até achar um do conjunto de sincronização `g`;
3. devolve um nó `erro` para a árvore, e a análise segue.

Os conjuntos de sincronização ficam em `recovery/Follow.java` e seguem a regra do modo pânico: os tokens de sincronismo de um não-terminal são o seu **Follow**.

| Onde | Conjunto de sincronização |
|---|---|
| `comando` | Follow(comando) = início de outro comando, `}` ou `fim_de_jogo` |
| condição do if/while/for | `{` (início do bloco), mais `}`, `fim_de_jogo` e os comandos de controle |
| cabeçalho `apito_inicial()` | início de um comando ou `fim_de_jogo` |
| nível do programa (ex.: `}` sobrando) | início de um comando, `fim_de_jogo` ou fim do arquivo |

Para evitar mensagens em cascata, um erro só é registrado se a sincronização parar num token diferente da anterior (`lastError`), e um erro sintático causado por um símbolo inválido já reportado pelo léxico não é repetido.

**Léxico.** A classe `LexicoComRecuperacao` estende o léxico gerado: ao encontrar um símbolo que não pertence à linguagem (ex.: `@`), registra o erro léxico, descarta só esse caractere e continua a leitura.

Exemplo com três erros:

```text
apito_inicial()
placar a = 1
placar b = 2 @ 3;
placar c = (4 + 5;
fim_de_jogo
```

```text
[FALTA GRAVE] O VAR encontrou 3 erros:
[SINTATICO] linha 3, coluna 1: Token inesperado: 'placar'. Esperado: ",", ";", <OP_REL>, <OP_SOMA>, <OP_MULT>.
[LEXICO] linha 3, coluna 14: Símbolo inválido: '@'. Esse símbolo não pertence ao alfabeto da linguagem Compilaço.
[SINTATICO] linha 4, coluna 18: Token inesperado: ';'. Esperado: ")", <OP_REL>, <OP_SOMA>, <OP_MULT>.
```

Com `--debug`, o passo a passo da recuperação também é exibido (conjunto de sincronização, tokens ignorados e token onde a análise retomou).

## Observação

Neste estágio o projeto realiza análise léxica e sintática. Ele ainda não interpreta/executa as instruções da linguagem nem possui análise semântica completa.
