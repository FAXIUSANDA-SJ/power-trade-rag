Set-Location "d:\project2\power-trade-rag"
$remote = git remote get-url origin 2>&1 | Out-String
$status = git status 2>&1 | Out-String
$log = git log --oneline -3 2>&1 | Out-String
$result = @"
Remote URL: $remote

Git Status:
$status

Git Log:
$log
"@
$result | Out-File -FilePath "git_info.txt" -Encoding utf8
$result
