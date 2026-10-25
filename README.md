# Jack Compiler – Syntax Analyzer

Syntax analyzer (tokenizer + parser → XML) for the Jack language (nand2tetris, Project 10).

## Requirements
- JDK 17+ (tested with 23)

## Build & run
```powershell
./build.ps1                       # compile into out/
./build.ps1 examples/Seven        # compile and run
```

## Layout
- `src/jack/` – Java sources (`Main`, then `Tokenizer`, `CompilationEngine`)
- `examples/` – `.jack` programs and expected `.xml` outputs
- Compare output with the nand2tetris `TextComparer` tool.

## Plan
1. Tokenizer → `XxxT.xml` (token stream)
2. CompilationEngine → `Xxx.xml` (parse tree)
