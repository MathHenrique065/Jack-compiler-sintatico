# Regression test: ./test.ps1
# - valid examples must generate XML identical to the reference files in examples/<Name>
# - invalid examples must be rejected (exit code 1)
$ErrorActionPreference = "Stop"
./build.ps1 | Out-Null
$fail = 0

foreach ($name in "Seven", "Average") {
    java -cp out jack.Main "examples/$name" "out/test/$name" | Out-Null
    java -cp out jack.Validate "out/test/$name" "examples/$name"
    if ($LASTEXITCODE -ne 0) { $fail++ }
}

foreach ($name in "Invalid", "InvalidLexical", "InvalidTrailing") {
    java -cp out jack.Main "examples/$name" "out/test/$name" 2>&1 | ForEach-Object { Write-Host "  $_" }
    if ($LASTEXITCODE -eq 1) { Write-Host "OK    $name rejeitado" } else { Write-Host "FALHA $name deveria ser rejeitado"; $fail++ }
}

if ($fail -gt 0) { Write-Host "$fail teste(s) falharam"; exit 1 }
Write-Host "Todos os testes passaram"
