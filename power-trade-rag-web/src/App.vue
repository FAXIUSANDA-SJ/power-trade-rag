<template>
  <div class="app-container">
    <!-- 侧边栏导航 -->
    <el-aside class="sidebar" width="220px">
      <div class="logo">
        <el-icon :size="24" color="#409EFF"><ChatDotSquare /></el-icon>
        <span class="logo-text">电力交易 RAG</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="sidebar-menu"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatLineRound /></el-icon>
          <span>智能问答</span>
        </el-menu-item>
        
        <el-menu-item index="/knowledge">
          <el-icon><Folder /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute === '/chat'">智能问答</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute === '/knowledge'">知识库管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute === '/documents'">文档管理</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" icon="User" />
              <span class="username">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotSquare, ChatLineRound, Folder, Document, User } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.path)
</script>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  background: #f0f2f5;
}

.sidebar {
  background: #304156;
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  background: #2b3a4b;
  gap: 10px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.sidebar-menu {
  border-right: none;
  margin-top: 10px;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 5px 10px;
  border-radius: 6px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #263445 !important;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #409EFF !important;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: #606266;
}

.username {
  font-size: 14px;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

:deep(.el-main) {
  background-color: #f0f2f5;
}
</style>
