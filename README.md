# Jack Compiler – Analisador Sintático

Analisador léxico (scanner) e sintático (parser) da linguagem **Jack** (nand2tetris, Projeto 10),
integrados em um único programa. Lê arquivos `.jack` e gera arquivos XML com a lista de tokens e a árvore sintática.

## Grupo
| Nome | Matrícula |
|------|-----------|
| _PREENCHER_ | _PREENCHER_ |

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

## Arquivos de saída XML
Para cada `Nome.jack` o programa gera, na mesma pasta:

- **`Nome.xml`** – **árvore sintática gerada pelo parser** (formato oficial nand2tetris).
- `NomeT.xml` – lista de tokens gerada pelo scanner.

Em caso de erro de sintaxe, a mensagem inclui o arquivo e a linha, por exemplo:
`examples\Invalid\Main.jack: Syntax error at line 4: expected a term but found ';'`
e o programa termina com código de saída 1.

## Estrutura do projeto
| Arquivo | Função |
|---------|--------|
| `src/jack/Main.java` | Ponto de entrada: arquivos/pastas, escrita dos XMLs, tratamento de erros |
| `src/jack/Tokenizer.java` | Scanner: palavras-chave, símbolos, constantes, identificadores, comentários |
| `src/jack/Token.java`, `TokenType.java` | Token (tipo, valor, linha) e categorias léxicas |
| `src/jack/CompilationEngine.java` | Parser descendente recursivo: uma função por regra da gramática |
| `examples/` | Programas `.jack` de teste e saídas geradas |

## Status da validação contra os arquivos oficiais
- [x] Scanner e parser executam sem erro nos exemplos `Seven` e `Average`.
- [x] Programa com erro (`examples/Invalid`) é rejeitado com mensagem e linha.
- [ ] **Comparação com os XMLs oficiais (TextComparer) – _PREENCHER após rodar a validação_**
  (ex.: ArrayTest, ExpressionLessSquare, Square: OK / diferenças encontradas).

## Desafios enfrentados
_PREENCHER pelo grupo, com suas próprias palavras._ Pontos que podem ser comentados:
- tratamento de comentários `//` e `/* */` dentro do scanner;
- escape de `<`, `>`, `&` e `"` na saída XML;
- decidir entre chamada de sub-rotina (`foo()` / `Obj.foo()`) e acesso a variável/array no `compileTerm`;
- indentação idêntica à dos arquivos oficiais.

## Plano
- [x] Tokenizer → `XxxT.xml` (lista de tokens)
- [x] CompilationEngine → `Xxx.xml` (árvore sintática)
- [ ] Validar contra os arquivos XML oficiais do nand2tetris
