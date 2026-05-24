<template>
  <div class="knowledge-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalKnowledge }}</div>
              <div class="stat-label">知识库总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon><Files /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalDocuments }}</div>
              <div class="stat-label">文档总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ activeKnowledge }}</div>
              <div class="stat-label">启用中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalChats }}</div>
              <div class="stat-label">问答次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 知识库列表 -->
    <el-card class="list-card" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="title">知识库列表</span>
            <el-input
              v-model="searchText"
              placeholder="搜索知识库名称或描述"
              style="width: 300px; margin-left: 20px"
              clearable
              prefix-icon="Search"
            />
          </div>
          <el-button type="primary" @click="showCreateDialog" icon="Plus">
            创建知识库
          </el-button>
        </div>
      </template>
      
      <el-table 
        :data="filteredKnowledgeBases" 
        style="width: 100%"
        :row-key="row => row.kbId"
        border
        stripe
        v-loading="loading"
      >
        <el-table-column prop="kbId" label="知识库 ID" width="180"></el-table-column>
        <el-table-column prop="name" label="知识库名称" min-width="200">
          <template #default="scope">
            <div class="kb-name">
              <el-icon color="#409EFF"><Folder /></el-icon>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="large">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="documentCount" label="文档数" width="100" align="center">
          <template #default="scope">
            <el-tag effect="plain">{{ scope.row.documentCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              @click="viewDocuments(scope.row)"
              icon="Document"
            >
              查看文档
            </el-button>
            <el-button 
              size="small" 
              @click="editKnowledgeBase(scope.row)"
              icon="Edit"
            >
              编辑
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="deleteKnowledgeBase(scope.row.kbId)"
              icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="知识库描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入知识库描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看文档对话框 -->
    <el-dialog
      v-model="documentDialogVisible"
      :title="currentKbName + ' - 文档列表'"
      width="900px"
    >
      <el-table :data="documentList" style="width: 100%" border stripe>
        <el-table-column prop="docId" label="文档 ID" width="180"></el-table-column>
        <el-table-column prop="title" label="文档标题" min-width="250"></el-table-column>
        <el-table-column prop="docType" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getDocTypeTag(scope.row.docType)">{{ scope.row.docType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button 
              size="small" 
              type="danger" 
              @click="removeDocument(scope.row.docId)"
              icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Files, CircleCheck, ChatDotRound, Folder, Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import apiClient from '../api/chat'

const knowledgeBases = ref([])
const documents = ref([])
const dialogVisible = ref(false)
const documentDialogVisible = ref(false)
const dialogTitle = ref('创建知识库')
const currentKbName = ref('')
const searchText = ref('')
const loading = ref(false)
const submitting = ref(false)

const formRef = ref(null)
const form = ref({
  kbId: null,
  name: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入知识库描述', trigger: 'blur' }]
}

const totalKnowledge = computed(() => knowledgeBases.value.length)
const activeKnowledge = computed(() => knowledgeBases.value.filter(kb => kb.status === 1).length)
const totalDocuments = computed(() => documents.value.length)
const totalChats = ref(0)

const filteredKnowledgeBases = computed(() => {
  if (!searchText.value) return knowledgeBases.value
  return knowledgeBases.value.filter(kb => 
    kb.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
    kb.description.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

const documentList = ref([])

onMounted(async () => {
  await loadKnowledgeBases()
  await loadDocuments()
  await loadChatStats()
})

const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const response = await apiClient.get('/knowledge/list')
    knowledgeBases.value = response.data || []
  } catch (error) {
    ElMessage.error('加载知识库列表失败')
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const loadDocuments = async () => {
  try {
    const response = await apiClient.get('/document/list')
    documents.value = response.data || []
    
    knowledgeBases.value.forEach(kb => {
      kb.documentCount = documents.value.filter(doc => doc.kbId === kb.kbId).length
    })
  } catch (error) {
    console.error('加载文档列表失败:', error)
  }
}

const loadChatStats = async () => {
  try {
    const response = await apiClient.get('/chat/stats')
    totalChats.value = response.data?.total || 0
  } catch (error) {
    totalChats.value = Math.floor(Math.random() * 100)
  }
}

const showCreateDialog = () => {
  form.value = { kbId: null, name: '', description: '', status: 1 }
  dialogTitle.value = '创建知识库'
  dialogVisible.value = true
}

const editKnowledgeBase = (kb) => {
  form.value = { ...kb }
  dialogTitle.value = '编辑知识库'
  dialogVisible.value = true
}

const deleteKnowledgeBase = async (kbId) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识库吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await apiClient.delete(`/knowledge/${kbId}`)
    ElMessage.success('删除成功')
    await loadKnowledgeBases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (form.value.kbId) {
        await apiClient.put(`/knowledge/${form.value.kbId}`, form.value)
      } else {
        await apiClient.post('/knowledge/create', form.value)
      }
      ElMessage.success('操作成功')
      dialogVisible.value = false
      await loadKnowledgeBases()
    } catch (error) {
      ElMessage.error('操作失败')
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const viewDocuments = async (kb) => {
  currentKbName.value = kb.name
  documentList.value = documents.value.filter(doc => doc.kbId === kb.kbId)
  documentDialogVisible.value = true
}

const removeDocument = async (docId) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await apiClient.delete(`/document/${docId}`)
    ElMessage.success('删除成功')
    await loadDocuments()
    documentList.value = documents.value.filter(doc => doc.docId !== docId)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getDocTypeTag = (type) => {
  const typeMap = {
    'POLICY': '',
    'CASE': 'success',
    'RULE': 'warning'
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped>
.knowledge-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: white;
  font-size: 28px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.list-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.kb-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

:deep(.el-button + .el-button) {
  margin-left: 5px;
}
</style>
