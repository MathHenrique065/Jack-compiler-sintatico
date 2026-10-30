# Regression test: ./test.ps1
# - official nand2tetris programs (tests/official) must generate XML equivalent to the official
#   reference files (ArrayTest, ExpressionLessSquare and Square: Main, Square, SquareGame)
# - our own valid examples must match their reference files in examples/<Name>
# - invalid examples must be rejected (exit code 1) with a "Linha N, coluna C" message
$ErrorActionPreference = "Stop"
./build.ps1 | Out-Null
$fail = 0

Write-Host "== Arquivos oficiais do nand2tetris (tests/official)"
foreach ($dir in Get-ChildItem tests/official -Directory) {
    java -cp out jack.Main $dir.FullName "out/test/official/$($dir.Name)" | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Host "FALHA $($dir.Name) nao foi analisado"; $fail++; continue }
    java -cp out jack.Validate "out/test/official/$($dir.Name)" $dir.FullName
    if ($LASTEXITCODE -ne 0) { $fail++ }
}

Write-Host "== Exemplos proprios (examples)"
foreach ($name in "Seven", "Average") {
    java -cp out jack.Main "examples/$name" "out/test/$name" | Out-Null
    java -cp out jack.Validate "out/test/$name" "examples/$name"
    if ($LASTEXITCODE -ne 0) { $fail++ }
}

Write-Host "== Programas invalidos devem ser rejeitados"
foreach ($name in "Invalid", "InvalidLexical", "InvalidTrailing") {
    $msg = java -cp out jack.Main "examples/$name" "out/test/$name" 2>&1 | Out-String
    $code = $LASTEXITCODE
    Write-Host "  $($msg.Trim())"
    if ($code -eq 1 -and $msg -match "Linha \d+, coluna \d+") { Write-Host "OK    $name rejeitado" }
    else { Write-Host "FALHA $name deveria ser rejeitado com linha/coluna"; $fail++ }
}

if ($fail -gt 0) { Write-Host "$fail teste(s) falharam"; exit 1 }
Write-Host "Todos os testes passaram"
exit 0
