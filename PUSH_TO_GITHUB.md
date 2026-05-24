# 📤 GitHub 推送指南

## ✅ 已完成的准备工作

您的项目已经成功初始化并配置好 Git：

- ✅ Git 仓库已初始化
- ✅ dev 分支已创建
- ✅ 代码已提交（commit: c94aa31）
- ✅ 远程仓库地址已配置：`https://github.com/FAXIUSANDA-SJ/power-trade-rag.git`
- ✅ .gitignore 文件已创建

## 🚀 推送到 GitHub 的步骤

### 步骤 1：在 GitHub 上创建仓库

1. **访问 GitHub 新建仓库页面**
   - 打开 https://github.com/new

2. **填写仓库信息**
   - **Repository name**: `power-trade-rag`
   - **Description**: `电力交易智能问答系统 - 基于 RAG 技术的企业级智能问答系统`
   - **Visibility**: 选择 Public（公开）或 Private（私有）
   - **不要勾选** "Add a README file"
   - **不要勾选** "Add .gitignore"
   - **不要勾选** "Choose a license"

3. **点击 "Create repository"**

### 步骤 2：推送代码到 GitHub

由于网络连接问题，您需要手动执行推送命令。

#### 方法 A：使用 HTTPS 推送（推荐）

打开命令行，执行：

```bash
cd d:\project2\power-trade-rag
git push -u origin dev
```

**认证方式：**
- 输入 GitHub 用户名：`FAXIUSANDA-SJ`
- 输入密码时使用 **Personal Access Token**（不是账号密码）

**如何获取 Personal Access Token：**
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 填写备注（如 "Git Push"）
4. 选择过期时间（建议 90 天）
5. 勾选 `repo` 权限
6. 点击 "Generate token"
7. **复制并保存 Token**（只显示一次！）

#### 方法 B：使用 SSH 推送

**配置 SSH 密钥：**

```bash
# 1. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "1113248369@qq.com"
# 连续按回车即可

# 2. 查看公钥内容
cat ~/.ssh/id_ed25519.pub

# 3. 添加公钥到 GitHub
# 访问 https://github.com/settings/keys
# 点击 "New SSH key"
# 粘贴公钥内容并保存

# 4. 更改远程仓库地址为 SSH
git remote set-url origin git@github.com:FAXIUSANDA-SJ/power-trade-rag.git

# 5. 推送
git push -u origin dev
```

### 步骤 3：验证推送成功

推送成功后，访问：
```
https://github.com/FAXIUSANDA-SJ/power-trade-rag
```

您应该能看到所有项目文件。

## 🔧 常见问题解决

### 问题 1：网络连接超时

**症状：**
```
fatal: unable to access 'https://github.com/...': Failed to connect to github.com port 443
```

**解决方案：**

1. **使用代理**（如果有）
   ```bash
   git config --global http.proxy http://127.0.0.1:7890
   git config --global https.proxy http://127.0.0.1:7890
   git push -u origin dev
   git config --global --unset http.proxy
   git config --global --unset https.proxy
   ```

2. **修改 DNS**
   - 将 DNS 服务器改为 `8.8.8.8` 或 `1.1.1.1`

3. **使用 GitHub Desktop**
   - 下载：https://desktop.github.com
   - 登录账号
   - File -> Add Local Repository -> 选择项目目录
   - 点击 Publish repository

### 问题 2：认证失败

**症状：**
```
remote: Invalid username or password.
```

**解决方案：**
- 使用 Personal Access Token 代替密码
- 检查用户名是否正确：`FAXIUSANDA-SJ`
- 确认 Token 权限包含 `repo`

### 问题 3：仓库不存在

**症状：**
```
remote: Repository not found.
```

**解决方案：**
- 确认已在 GitHub 上创建了 `power-trade-rag` 仓库
- 检查仓库所有者是否为 `FAXIUSANDA-SJ`

## 📊 项目信息

- **项目名称**: power-trade-rag
- **当前分支**: dev
- **提交数量**: 1
- **最新提交**: feat: 电力交易智能问答系统初始提交
- **包含内容**:
  - 后端代码（Spring Boot + LangChain4j）
  - 前端代码（Vue3 + Element Plus）
  - 向量数据库集成（ChromaDB + Milvus）
  - 20 份电力交易测试文档
  - Docker 配置
  - 完整文档

## 📝 快速推送脚本

您可以直接运行提供的批处理脚本：

```bash
push-to-github.bat
```

脚本会引导您完成推送过程。

## 🎯 推送后的下一步

1. **验证代码**
   - 在 GitHub 上查看文件是否完整
   - 检查 dev 分支是否正确

2. **创建 Pull Request**（可选）
   - 如果要合并到 main 分支
   - 创建 PR 并审查代码

3. **克隆到其他设备**
   ```bash
   git clone -b dev https://github.com/FAXIUSANDA-SJ/power-trade-rag.git
   ```

---

**更新时间**: 2026-05-08  
**作者**: FAXIUSANDA-SJ  
**联系**: 1113248369@qq.com
