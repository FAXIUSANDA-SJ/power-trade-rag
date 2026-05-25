<template>
  <div class="document-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文档管理</span>
        </div>
      </template>
      
      <div class="upload-section">
        <el-upload
          class="upload-demo"
          drag
          action="/api/document/upload"
          multiple
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持PDF、Word、Excel等格式文件</div>
          </template>
        </el-upload>
      </div>
      
      <div class="document-list">
        <div class="toolbar">
          <el-select v-model="filters.ingestStatus" placeholder="按摄取状态筛选" clearable @change="loadDocuments">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="running" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
          </el-select>
        </div>
        <el-table :data="documents" style="width: 100%">
          <el-table-column prop="docId" label="文档ID" width="180"></el-table-column>
          <el-table-column prop="title" label="文档标题"></el-table-column>
          <el-table-column prop="docType" label="类型" width="100"></el-table-column>
          <el-table-column prop="ingestStatus" label="摄取状态" width="120"></el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button 
                size="small" 
                type="danger" 
                @click="deleteDocument(scope.row.docId)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { documentApi } from '../api/document'

const documents = ref([])
const filters = ref({
  ingestStatus: ''
})

onMounted(() => {
  loadDocuments()
})

const loadDocuments = async () => {
  try {
    const response = await documentApi.getList({
      page: 1,
      size: 10,
      ingestStatus: filters.value.ingestStatus || undefined
    })
    documents.value = response.data || []
  } catch (error) {
    console.error('加载文档列表失败:', error)
    ElMessage.error('加载文档列表失败')
  }
}

const handleUploadSuccess = (response) => {
  ElMessage.success(response?.message || '文件上传已受理')
  loadDocuments()
}

const handleUploadError = (error) => {
  ElMessage.error('文件上传失败')
  console.error('上传失败:', error)
}

const deleteDocument = async (docId) => {
  try {
    await documentApi.delete(docId)
    ElMessage.success('文档删除成功')
    loadDocuments()
  } catch (error) {
    ElMessage.error('文档删除失败')
    console.error('删除失败:', error)
  }
}
</script>

<style scoped>
.document-management {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.upload-section {
  margin-bottom: 20px;
}

.document-list {
  margin-top: 20px;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
