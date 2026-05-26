<template>
  <div class="document-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文档管理</span>
          <div class="header-actions">
            <el-tag size="small" :type="shouldAutoRefresh ? 'success' : 'info'">
              {{ shouldAutoRefresh ? '任务处理中，自动刷新中' : '当前无活动任务' }}
            </el-tag>
            <el-button link type="primary" @click="loadPageData">
              手动刷新
            </el-button>
          </div>
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

      <div class="ocr-panel">
        <div class="panel-header">
          <span>OCR 调试面板</span>
          <span class="panel-tip">用于验证当前 OCR provider、字段映射和错误响应</span>
        </div>
        <div class="ocr-toolbar">
          <el-upload
            class="ocr-upload"
            :auto-upload="false"
            :limit="1"
            :show-file-list="true"
            :on-change="handleOcrFileChange"
            :on-remove="handleOcrFileRemove"
          >
            <el-button type="primary" plain>选择测试文件</el-button>
          </el-upload>
          <el-button type="primary" :loading="ocrTesting" @click="runOcrTest">
            开始 OCR 测试
          </el-button>
          <el-button @click="clearOcrResult">清空结果</el-button>
        </div>
        <div v-if="ocrResult" class="ocr-result">
          <div class="ocr-metrics">
            <div class="detail-item">
              <span class="detail-label">Provider</span>
              <span>{{ ocrResult.provider || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">文件名</span>
              <span>{{ ocrResult.fileName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">文件大小</span>
              <span>{{ formatFileSize(ocrResult.fileSize) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">执行结果</span>
              <el-tag :type="ocrResult.success ? 'success' : 'danger'">
                {{ ocrResult.success ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">文本长度</span>
              <span>{{ ocrResult.textLength || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">耗时</span>
              <span>{{ ocrResult.durationMs || 0 }} ms</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">HTTP 状态</span>
              <span>{{ ocrResult.httpStatus || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">错误类型</span>
              <span>{{ ocrResult.errorType || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">错误编码</span>
              <span>{{ ocrResult.errorCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">内容类型</span>
              <span>{{ ocrResult.contentType || '-' }}</span>
            </div>
          </div>
          <div v-if="ocrResult.errorMessage" class="ocr-error">
            <div class="error-title">错误信息</div>
            <div class="error-message">{{ ocrResult.errorMessage }}</div>
          </div>
          <div v-if="ocrResult.responsePreview" class="ocr-preview">
            <div class="error-title">响应摘要</div>
            <div class="preview-content">{{ ocrResult.responsePreview }}</div>
          </div>
          <div class="ocr-text">
            <div class="error-title">识别文本</div>
            <pre class="text-content">{{ ocrResult.extractedText || '暂无识别文本' }}</pre>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stats-grid">
          <div class="stats-card">
            <div class="stats-label">任务总数</div>
            <div class="stats-value">{{ taskStats.total || 0 }}</div>
          </div>
          <div class="stats-card pending">
            <div class="stats-label">待处理</div>
            <div class="stats-value">{{ taskStats.pending || 0 }}</div>
          </div>
          <div class="stats-card running">
            <div class="stats-label">处理中</div>
            <div class="stats-value">{{ taskStats.running || 0 }}</div>
          </div>
          <div class="stats-card success">
            <div class="stats-label">成功</div>
            <div class="stats-value">{{ taskStats.success || 0 }}</div>
          </div>
          <div class="stats-card failed">
            <div class="stats-label">失败</div>
            <div class="stats-value">{{ taskStats.failed || 0 }}</div>
          </div>
        </div>
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
        <el-table v-loading="tableLoading" :data="documents" style="width: 100%">
          <el-table-column prop="docId" label="文档ID" width="180"></el-table-column>
          <el-table-column prop="title" label="文档标题"></el-table-column>
          <el-table-column prop="docType" label="类型" width="100"></el-table-column>
          <el-table-column prop="ingestTaskId" label="任务ID" width="210">
            <template #default="scope">
              {{ scope.row.ingestTaskId || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="ingestStatus" label="摄取状态" width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.ingestStatus)">
                {{ statusText(scope.row.ingestStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button
                v-if="scope.row.ingestTaskId"
                size="small"
                @click="showTaskDetail(scope.row)"
              >
                任务详情
              </el-button>
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

    <el-dialog v-model="taskDialogVisible" title="摄取任务详情" width="620px">
      <div v-loading="taskDetailLoading" class="task-detail">
        <template v-if="selectedTask">
          <div class="task-detail-grid">
            <div class="detail-item">
              <span class="detail-label">任务ID</span>
              <span>{{ selectedTask.taskId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">文档ID</span>
              <span>{{ selectedTask.docId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">任务类型</span>
              <span>{{ selectedTask.taskType || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">当前状态</span>
              <el-tag :type="statusType(selectedTask.status)">
                {{ statusText(selectedTask.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">重试次数</span>
              <span>{{ selectedTask.retryCount || 0 }} / {{ selectedTask.maxRetryCount || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">更新时间</span>
              <span>{{ formatTime(selectedTask.updateTime) }}</span>
            </div>
          </div>

          <div v-if="selectedTask.errorDetail" class="error-panel">
            <div class="error-title">失败详情</div>
            <div class="error-row">
              <span class="detail-label">错误分类</span>
              <span>{{ selectedTask.errorDetail.category || 'UNKNOWN' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">错误摘要</span>
              <span>{{ selectedTask.errorDetail.summary || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">完整信息</span>
              <span class="error-message">{{ selectedTask.errorDetail.message || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">是否可重试</span>
              <span>{{ selectedTask.errorDetail.canRetry ? '可重试' : '不可重试' }}</span>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="taskDialogVisible = false">关闭</el-button>
        <el-button
          v-if="selectedTask?.errorDetail?.canRetry"
          type="primary"
          @click="retrySelectedTask"
        >
          重试任务
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { documentApi } from '../api/document'

const documents = ref([])
const tableLoading = ref(false)
const taskStats = ref({
  total: 0,
  pending: 0,
  running: 0,
  success: 0,
  failed: 0
})
const taskDialogVisible = ref(false)
const taskDetailLoading = ref(false)
const selectedTask = ref(null)
const ocrTestFile = ref(null)
const ocrTesting = ref(false)
const ocrResult = ref(null)
const filters = ref({
  ingestStatus: ''
})
let refreshTimer = null

const shouldAutoRefresh = computed(() =>
  Number(taskStats.value.pending || 0) > 0 || Number(taskStats.value.running || 0) > 0
)

onMounted(() => {
  loadPageData()
  startAutoRefresh()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
})

const loadDocuments = async () => {
  try {
    tableLoading.value = true
    const response = await documentApi.getList({
      page: 1,
      size: 10,
      ingestStatus: filters.value.ingestStatus || undefined
    })
    documents.value = response.data || []
  } catch (error) {
    console.error('加载文档列表失败:', error)
    ElMessage.error('加载文档列表失败')
  } finally {
    tableLoading.value = false
  }
}

const loadTaskStats = async () => {
  const response = await documentApi.getTaskStats({})
  taskStats.value = {
    total: response.data?.total || 0,
    pending: response.data?.pending || 0,
    running: response.data?.running || 0,
    success: response.data?.success || 0,
    failed: response.data?.failed || 0
  }
}

const loadPageData = async (showError = true) => {
  try {
    await Promise.all([loadDocuments(), loadTaskStats()])
  } catch (error) {
    if (showError) {
      ElMessage.error('加载页面数据失败')
    }
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => {
    if (shouldAutoRefresh.value) {
      loadPageData(false)
    }
  }, 5000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const handleUploadSuccess = (response) => {
  ElMessage.success(response?.message || '文件上传已受理')
  loadPageData(false)
}

const handleUploadError = (error) => {
  ElMessage.error('文件上传失败')
  console.error('上传失败:', error)
}

const deleteDocument = async (docId) => {
  try {
    await documentApi.delete(docId)
    ElMessage.success('文档删除成功')
    loadPageData(false)
  } catch (error) {
    ElMessage.error('文档删除失败')
    console.error('删除失败:', error)
  }
}

const showTaskDetail = async (row) => {
  if (!row.ingestTaskId) {
    return
  }
  taskDialogVisible.value = true
  taskDetailLoading.value = true
  selectedTask.value = null
  try {
    const response = await documentApi.getTaskDetail(row.ingestTaskId)
    selectedTask.value = response.data
  } catch (error) {
    console.error('加载任务详情失败:', error)
    ElMessage.error('加载任务详情失败')
  } finally {
    taskDetailLoading.value = false
  }
}

const retrySelectedTask = async () => {
  if (!selectedTask.value?.taskId) {
    return
  }
  try {
    await documentApi.retryTask(selectedTask.value.taskId)
    ElMessage.success('任务已重新提交')
    taskDialogVisible.value = false
    await loadPageData(false)
  } catch (error) {
    console.error('重试任务失败:', error)
    ElMessage.error('重试任务失败')
  }
}

const handleOcrFileChange = (uploadFile) => {
  ocrTestFile.value = uploadFile?.raw || null
}

const handleOcrFileRemove = () => {
  ocrTestFile.value = null
}

const clearOcrResult = () => {
  ocrResult.value = null
  ocrTestFile.value = null
}

const runOcrTest = async () => {
  if (!ocrTestFile.value) {
    ElMessage.warning('请先选择一个测试文件')
    return
  }
  const formData = new FormData()
  formData.append('file', ocrTestFile.value)
  try {
    ocrTesting.value = true
    const response = await documentApi.testOcr(formData)
    ocrResult.value = response.data || null
    ElMessage.success(response.data?.success ? 'OCR 测试成功' : 'OCR 测试已完成，请检查结果')
  } catch (error) {
    console.error('OCR 测试失败:', error)
    ElMessage.error('OCR 测试失败')
  } finally {
    ocrTesting.value = false
  }
}

const statusType = (status) => {
  switch (status) {
    case 'success':
      return 'success'
    case 'failed':
      return 'danger'
    case 'running':
      return 'warning'
    default:
      return 'info'
  }
}

const statusText = (status) => {
  switch (status) {
    case 'pending':
      return '待处理'
    case 'running':
      return '处理中'
    case 'success':
      return '成功'
    case 'failed':
      return '失败'
    default:
      return status || '-'
  }
}

const formatTime = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

const formatFileSize = (value) => {
  if (!value && value !== 0) {
    return '-'
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(2)} KB`
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`
}
</script>

<style scoped>
.document-management {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-section {
  margin-bottom: 20px;
}

.ocr-panel {
  margin-bottom: 20px;
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-tip {
  font-size: 12px;
  color: #909399;
}

.ocr-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ocr-result {
  margin-top: 16px;
}

.ocr-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ocr-error,
.ocr-preview,
.ocr-text {
  margin-top: 16px;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.preview-content,
.text-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stats-card {
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;
}

.stats-card.pending {
  background: #fdf6ec;
}

.stats-card.running {
  background: #ecf5ff;
}

.stats-card.success {
  background: #f0f9eb;
}

.stats-card.failed {
  background: #fef0f0;
}

.stats-label {
  font-size: 13px;
  color: #606266;
}

.stats-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.document-list {
  margin-top: 20px;
}

.toolbar {
  margin-bottom: 16px;
}

.task-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-item,
.error-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
}

.error-panel {
  margin-top: 20px;
  padding: 16px;
  border-radius: 8px;
  background: #fef0f0;
}

.error-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #c45656;
}

.error-message {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .ocr-metrics,
  .stats-grid,
  .task-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .ocr-metrics,
  .stats-grid,
  .task-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
