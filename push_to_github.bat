@echo off
cd /d d:\project2\power-trade-rag

echo ================================
echo Step 1: Check remote URL
echo ================================
git remote get-url origin

echo.
echo ================================
echo Step 2: Check git status
echo ================================
git status

echo.
echo ================================
echo Step 3: Push to GitHub
echo ================================
git push -u origin dev --force

echo.
echo ================================
echo Done! Exit code: %errorlevel%
echo ================================
pause
