# Usage:
#   ./build.ps1                      # compile only
#   ./build.ps1 examples/Square      # compile and run on a .jack file/folder
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
javac -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Build OK"
if ($args.Count -gt 0) { java -cp out jack.Main $args[0] }
