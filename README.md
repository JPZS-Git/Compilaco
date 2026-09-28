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
│       │       ├── Compilaco.java
│       │       ├── CompilacoConstants.java
│       │       ├── CompilacoTokenManager.java
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
parser.Compilaco
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
run-valid.bat
```

Para testar o erro sintático:

```bat
run-error.bat
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

Execute:

```text
java -cp out parser.Compilaco --debug
```

O programa começa a análise automaticamente quando encontra uma linha contendo:

```text
fim_de_jogo
```

Não é necessário enviar EOF manualmente.

## Observação

Neste estágio o projeto realiza análise léxica e sintática. Ele ainda não interpreta/executa as instruções da linguagem nem possui análise semântica completa.
