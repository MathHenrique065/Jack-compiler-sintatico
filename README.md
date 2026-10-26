# Jack Compiler – Analisador Sintático

Analisador léxico (scanner) e sintático (parser) da linguagem **Jack** (nand2tetris, Projeto 10),
integrados em um único programa. Lê arquivos `.jack` e gera arquivos XML com a lista de tokens e a árvore sintática.

## Grupo
| Nome | Matrícula |
|------|-----------|
| MATHEUS HENRIQUE ALBUQUERQUE TORRES | _PREENCHER_ |
| JOSE AUGUSTO SANTOS LOPES | _PREENCHER_ |

## Linguagem utilizada
**Java** (JDK 17 ou superior; testado com JDK 23). Sem dependências externas.

## Como compilar e executar

Windows (PowerShell):
```powershell
./build.ps1                         # apenas compila para out/
./build.ps1 examples/Average        # compila e executa sobre uma pasta
```

Manualmente (qualquer sistema):
```bash
javac -d out src/jack/*.java
java -cp out jack.Main <Arquivo.jack | pasta>
java -cp out jack.Main --help
```

## Exemplo de uso
```bash
java -cp out jack.Main examples/Seven/Main.jack
```
Saída:
```
Tokenized examples\Seven\Main.jack -> examples\Seven\MainT.xml (27 tokens)
Parsed    examples\Seven\Main.jack -> examples\Seven\Main.xml
```

Passando uma pasta, todos os `.jack` dentro dela são processados.

Com um segundo argumento, a saída vai para outro lugar (arquivo para uma entrada única, pasta para uma pasta):
```bash
java -cp out jack.Main examples/Seven/Main.jack saida/Seven.xml   # gera saida/Seven.xml e saida/SevenT.xml
java -cp out jack.Main examples/Average saida                     # gera saida/Main.xml e saida/MainT.xml
```

## Arquivos de saída XML
Para cada `Nome.jack` o programa gera, na mesma pasta:

- **`Nome.xml`** – **árvore sintática gerada pelo parser** (formato oficial nand2tetris).
- `NomeT.xml` – lista de tokens gerada pelo scanner.

Em caso de erro de sintaxe, o parser para no **primeiro erro** (sem recuperação) e emite, no formato do livro:
`Linha N: Esperado 'X' encontrado 'Y'`, precedido do arquivo, por exemplo:
`examples\Invalid\Main.jack: Linha 4: Esperado 'term' encontrado ';'`
e o programa termina com código de saída 1.

A implementação segue o capítulo
[5. Analisador sintático](https://lambdageo-edu.github.io/book-compiladores-com-nand2tetris/unidade-1-frontend/05-analisador-sintatico/):
parser preditivo recursivo (top-down, LL(1) com fatoração à esquerda do identificador em `term`),
um método `parseX` por não-terminal, e as mesmas tags XML da especificação do Nand2Tetris.

## Estrutura do projeto
| Arquivo | Função |
|---------|--------|
| `src/jack/Main.java` | Ponto de entrada: arquivos/pastas, escrita dos XMLs, tratamento de erros |
| `src/jack/Tokenizer.java` | Scanner: palavras-chave, símbolos, constantes, identificadores, comentários |
| `src/jack/Token.java`, `TokenType.java` | Token (tipo, valor, linha) e categorias léxicas |
| `src/jack/Parser.java` | Parser descendente recursivo: `parse()`, `parseClass`, `parseTerm`, ... e `XMLOutput()` |
| `src/jack/Validate.java` | Compara o XML gerado com os arquivos oficiais (ignora indentação e linhas vazias) |
| `examples/` | Programas `.jack` de teste e saídas geradas |

## Status da validação contra os arquivos oficiais
- [x] Scanner e parser executam sem erro nos exemplos `Seven` e `Average`.
- [x] Programa com erro (`examples/Invalid`) é rejeitado com mensagem e linha.
- [ ] **Comparação com os XMLs oficiais – _PREENCHER após rodar a validação_**
  (ex.: ArrayTest, ExpressionLessSquare, Square: OK / diferenças encontradas).

Como validar (os `.jack` e os `.xml` oficiais ficam na pasta do Nand2Tetris; a saída vai para outra pasta):
```bash
java -cp out jack.Main     <pasta_nand2tetris>/Square saida/Square         # gera a saída sem tocar nos oficiais
java -cp out jack.Validate saida/Square <pasta_nand2tetris>/Square         # compara cada *.xml oficial
```
A ferramenta compara linha a linha após `strip` e remoção de linhas vazias, como no teste do livro, e retorna código 1 se houver diferença.
## Desafios enfrentados
_PREENCHER pelo grupo, com suas próprias palavras._ Pontos que podem ser comentados:
- tratamento de comentários `//` e `/* */` dentro do scanner;
- escape de `<`, `>`, `&` e `"` na saída XML;
- decidir entre chamada de sub-rotina (`foo()` / `Obj.foo()`) e acesso a variável/array no `parseTerm` (lookahead);
- indentação idêntica à dos arquivos oficiais.

## Plano
- [x] Tokenizer → `XxxT.xml` (lista de tokens)
- [x] Parser → `Xxx.xml` (árvore sintática)
- [ ] Validar contra os arquivos XML oficiais do nand2tetris
