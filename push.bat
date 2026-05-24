@echo off
cd /d D:\project2\power-trade-rag
git config --global credential.helper store
git push -u origin dev 2>push_error.txt >push_output.txt
echo Exit Code: %errorlevel%
echo Output:
type push_output.txt
echo Error:
type push_error.txt
