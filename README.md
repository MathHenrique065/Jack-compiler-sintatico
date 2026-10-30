# Jack Compiler – Analisador Sintático

Analisador léxico (scanner) e sintático (parser) da linguagem **Jack** (nand2tetris, Projeto 10),
integrados em um único programa. Lê arquivos `.jack` e gera arquivos XML com a lista de tokens e a árvore sintática.

## Grupo
| Nome | Matrícula |
|------|-----------|
| MATHEUS HENRIQUE ALBUQUERQUE TORRES | 2020002089 |

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

Execução dos testes automatizados:
```powershell
pwsh -ExecutionPolicy Bypass -File ./test.ps1
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
Para cada `Nome.jack` o programa gera, na mesma pasta (ou no caminho de destino fornecido):

- **`Nome.xml`** – **árvore sintática gerada pelo parser** (formato oficial nand2tetris).
- `NomeT.xml` – lista de tokens gerada pelo scanner.

Em caso de erro de sintaxe, o parser para no **primeiro erro** (sem recuperação) e emite mensagem clara com linha e coluna:
`Linha N, coluna C: Esperado 'X' encontrado 'Y'`, precedido do arquivo, por exemplo:
`examples\Invalid\Main.jack: Linha 4, coluna 17: Esperado 'term' encontrado ';'`
e o programa termina com código de saída 1.

O scanner também rejeita com `Linha N, coluna C: ...`: comentário `/*` sem fechamento, string sem aspas de fechamento ou com quebra de linha, inteiro fora de 0..32767 e caractere inesperado. O parser rejeita tokens sobrando depois do `}` final da classe.

### Métodos auxiliares de consumo de tokens
Conforme solicitado na especificação, o parser implementa os métodos auxiliares:
- `peek()`: inspeciona o token atual (lookahead) sem consumi-lo.
- `advance()`: grava o token atual na árvore XML e avança a posição de leitura.
- `match(TokenType)` / `match(TokenType, String...)`: verifica se o token atual é do tipo esperado.
- `consume(TokenType)` / `consume(TokenType, String...)`: consome o token se coincidir com o esperado, ou lança exceção com linha e coluna.

A implementação segue o capítulo
[5. Analisador sintático](https://lambdageo-edu.github.io/book-compiladores-com-nand2tetris/unidade-1-frontend/05-analisador-sintatico/):
parser preditivo recursivo (top-down, LL(1) com fatoração à esquerda do identificador em `term`),
um método `parseX` por não-terminal, e as mesmas tags XML da especificação do Nand2Tetris.

## Estrutura do projeto
| Arquivo | Função |
|---------|--------|
| `src/jack/Main.java` | Ponto de entrada: arquivos/pastas, escrita dos XMLs, tratamento de erros |
| `src/jack/Tokenizer.java` | Scanner: palavras-chave, símbolos, constantes, identificadores, comentários |
| `src/jack/Token.java`, `TokenType.java` | Token (tipo, valor, linha, coluna) e categorias léxicas |
| `src/jack/Parser.java` | Parser descendente recursivo: `parse()`, `parseClass`, `parseTerm`, ... e `XMLOutput()` |
| `src/jack/Validate.java` | Compara o XML gerado com os arquivos oficiais (ignora indentação e linhas vazias) |
| `test.ps1` | Teste de regressão completo: compara contra oficiais, exemplos e validação de erros |
| `tests/official/` | Arquivos oficiais de teste do nand2tetris (`ArrayTest`, `ExpressionLessSquare`, `Square`) |
| `examples/` | Programas `.jack` de teste (`Seven`, `Average` com referência; `Invalid*` com erros) |

## Status da validação contra os arquivos oficiais
- [x] Scanner e parser executam sem erro nos exemplos `Seven` e `Average`.
- [x] Programas com erro (`examples/Invalid*`) são rejeitados com mensagem e indicação de linha e coluna.
- [x] `./test.ps1` roda toda a suíte de testes automaticamente.
- [x] **Comparação com os XMLs oficiais do nand2tetris**: 100% equivalente (10/10 arquivos XML validados com sucesso):
  - `tests/official/ArrayTest/Main.xml`: **OK**
  - `tests/official/ExpressionLessSquare/Main.xml`: **OK**
  - `tests/official/ExpressionLessSquare/Square.xml`: **OK**
  - `tests/official/ExpressionLessSquare/SquareGame.xml`: **OK**
  - `tests/official/Square/Main.xml`: **OK**
  - `tests/official/Square/Square.xml`: **OK**
  - `tests/official/Square/SquareGame.xml`: **OK**

Como validar manualmente (os `.jack` e os `.xml` oficiais ficam na pasta do Nand2Tetris; a saída vai para outra pasta):
```bash
java -cp out jack.Main     tests/official/Square saida/Square         # gera a saída sem tocar nos oficiais
java -cp out jack.Validate saida/Square tests/official/Square         # compara cada *.xml oficial
```
A ferramenta compara linha a linha após `strip` e remoção de linhas vazias, como no teste do livro, e retorna código 1 se houver diferença.

## Desafios enfrentados
- **Ambiguidade e Lookahead em `term`**: Na gramática Jack, `varName`, `varName[expr]` e chamadas de sub-rotina (`subroutineName(exprList)` e `(className|varName).subroutineName(exprList)`) começam todos com um identificador. O parser precisa de lookahead de 2 símbolos para distinguir. Para manter a estrutura LL(1) sem backtracking, foi aplicada fatoração à esquerda: consome-se o identificador inicial em `parseTerm` e inspeciona-se o próximo símbolo (`[`, `(`, `.`) para direcionar a derivação correta.
- **Precedência e Associatividade de Operadores**: A especificação de Jack define que todas as operações binárias têm a mesma precedência e associatividade estrita à esquerda (avaliadas na ordem em que aparecem). Isso simplifica a gramática em `term (op term)*`, permitindo um único laço `while (isOp())`, dispensando hierarquias complexas de expressões.
- **Rastreamento Preciso de Linha e Coluna**: O scanner calcula a posição de cada token incrementalmente (em tempo linear), garantindo que erros sintáticos ou léxicos apontem com exatidão onde o caractere ou token inesperado ocorreu.
- **Escape de Entidades XML**: Caracteres especiais como `<`, `>`, `&` e `"` exigiram substituição pelas entidades correspondentes (`&lt;`, `&gt;`, `&amp;`, `&quot;`) para que o XML fosse estritamente idêntico ao esperado pelas ferramentas do nand2tetris.
- **Tags Vazias Obrigatórias**: Regras como `<parameterList>` e `<expressionList>` devem gerar suas tags de abertura e fechamento mesmo quando não contêm elementos, detalhe essencial para compatibilidade com o validador oficial.

## Plano
- [x] Tokenizer → `XxxT.xml` (lista de tokens)
- [x] Parser → `Xxx.xml` (árvore sintática)
- [x] Validar contra os arquivos XML oficiais do nand2tetris
