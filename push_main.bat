@echo off
cd /d d:\project2\power-trade-rag

echo ================================
echo Pushing to main branch...
echo ================================

git checkout main 2>nul
if %errorlevel% neq 0 (
    echo Creating main branch...
    git branch -M main
)

git push -u origin main --force

echo.
echo ================================
echo Done! Exit code: %errorlevel%
echo ================================
