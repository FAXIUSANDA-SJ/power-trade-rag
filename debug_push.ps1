Set-Location "d:\project2\power-trade-rag"
Write-Host "Testing git ls-remote..."
$test = git ls-remote origin 2>&1
Write-Host "ls-remote result: $test"
Write-Host ""
Write-Host "Testing push..."
$push = git push -u origin dev --force 2>&1
Write-Host "Push result: $push"
Write-Host ""
Write-Host "Exit code: $LASTEXITCODE"
