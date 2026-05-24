@echo off
echo ========================================
echo 电力交易 RAG 项目 - GitHub 推送脚本
echo ========================================
echo.
echo 当前目录：%CD%
echo.
echo 请按照以下步骤操作：
echo.
echo 步骤 1: 在 GitHub 上创建仓库
echo   1. 访问 https://github.com/new
echo   2. 仓库名称：power-trade-rag
echo   3. 选择公开或私有
echo   4. 不要勾选 "Add a README file"
echo   5. 点击 "Create repository"
echo.
echo 步骤 2: 推送代码到 GitHub
echo.
echo 请选择推送方式：
echo   1. HTTPS 推送（需要账号密码或 Token）
echo   2. SSH 推送（需要配置 SSH 密钥）
echo   3. 退出
echo.
set /p choice="请输入选择 (1-3): "

if "%choice%"=="1" (
    echo.
    echo 使用 HTTPS 推送...
    echo 注意：推送时可能需要输入 GitHub 账号和密码
    echo.
    git push -u origin dev
    if errorlevel 1 (
        echo.
        echo 推送失败！可能原因：
        echo 1. 网络连接问题
        echo 2. 仓库不存在
        echo 3. 认证失败
        echo.
        echo 建议：
        echo - 检查网络连接
        echo - 确认已在 GitHub 创建仓库
        echo - 使用 Personal Access Token 代替密码
    ) else (
        echo.
        echo 推送成功！
        echo 请访问 https://github.com/FAXIUSANDA-SJ/power-trade-rag 查看代码
    )
) else if "%choice%"=="2" (
    echo.
    echo 使用 SSH 推送...
    echo.
    echo 首先配置 SSH 密钥：
    echo 1. 生成密钥：ssh-keygen -t ed25519 -C "1113248369@qq.com"
    echo 2. 复制公钥：cat ~/.ssh/id_ed25519.pub
    echo 3. 添加到 GitHub: https://github.com/settings/keys
    echo.
    set /p configured="是否已配置 SSH 密钥？(y/n): "
    if /i "%configured%"=="y" (
        git remote set-url origin git@github.com:FAXIUSANDA-SJ/power-trade-rag.git
        git push -u origin dev
        if errorlevel 1 (
            echo.
            echo 推送失败！请检查 SSH 配置
        ) else (
            echo.
            echo 推送成功！
        )
    ) else (
        echo.
        echo 请先配置 SSH 密钥，然后重新运行此脚本
    )
) else (
    echo.
    echo 已退出
)

echo.
echo ========================================
echo 按任意键退出...
pause >nul
