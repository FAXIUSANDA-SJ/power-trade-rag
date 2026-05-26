<template>
  <div class="task-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务中心</span>
          <div class="header-actions">
            <el-tag size="small" :type="shouldAutoRefresh ? 'success' : 'info'">
              {{ shouldAutoRefresh ? '检测到活动任务，自动刷新中' : '当前无活动任务' }}
            </el-tag>
            <el-button link type="primary" @click="loadPageData">
              手动刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="stats-grid">
        <div class="stats-card">
          <div class="stats-label">总任务数</div>
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

      <div class="toolbar">
        <el-select v-model="filters.status" placeholder="按状态筛选" clearable @change="handleQueryChange">
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="running" />
          <el-option label="成功" value="success" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-input
          v-model="filters.kbId"
          placeholder="输入知识库 ID"
          clearable
          class="kb-input"
          @keyup.enter="handleQueryChange"
          @clear="handleQueryChange"
        />
        <el-button type="primary" @click="handleQueryChange">查询</el-button>
      </div>

      <el-table v-loading="tableLoading" :data="tasks" style="width: 100%">
        <el-table-column prop="taskId" label="任务ID" min-width="220" />
        <el-table-column prop="docId" label="文档ID" min-width="180" />
        <el-table-column prop="kbId" label="知识库" min-width="140">
          <template #default="scope">
            {{ scope.row.kbId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="任务类型" width="120">
          <template #default="scope">
            {{ scope.row.taskType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">
              {{ statusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重试" width="110">
          <template #default="scope">
            {{ scope.row.retryCount || 0 }} / {{ scope.row.maxRetryCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="自动重试" width="130">
          <template #default="scope">
            <el-tag :type="scope.row.autoRetryAllowed ? 'success' : 'info'">
              {{ scope.row.autoRetryAllowed ? '允许' : '阻塞' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下次重试时间" width="190">
          <template #default="scope">
            {{ formatTime(scope.row.nextRetryTime) }}
          </template>
        </el-table-column>
        <el-table-column label="退避状态" width="140">
          <template #default="scope">
            <el-tag :type="scope.row.retryReady ? 'success' : 'warning'">
              {{ scope.row.retryReady ? '可执行' : '等待中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showTaskDetail(scope.row.taskId)">
              详情
            </el-button>
            <el-button
              v-if="scope.row.errorDetail?.canRetry"
              link
              type="warning"
              @click="retryTask(scope.row.taskId)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="loadTasks"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="任务详情" width="680px">
      <div v-loading="detailLoading" class="task-detail">
        <template v-if="currentTask">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">任务ID</span>
              <span>{{ currentTask.taskId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">文档ID</span>
              <span>{{ currentTask.docId || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">知识库</span>
              <span>{{ currentTask.kbId || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <el-tag :type="statusType(currentTask.status)">
                {{ statusText(currentTask.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">任务类型</span>
              <span>{{ currentTask.taskType || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">创建时间</span>
              <span>{{ formatTime(currentTask.createTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">更新时间</span>
              <span>{{ formatTime(currentTask.updateTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">重试次数</span>
              <span>{{ currentTask.retryCount || 0 }} / {{ currentTask.maxRetryCount || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">自动重试</span>
              <el-tag :type="currentTask.autoRetryAllowed ? 'success' : 'info'">
                {{ currentTask.autoRetryAllowed ? '允许' : '阻塞' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">退避状态</span>
              <el-tag :type="currentTask.retryReady ? 'success' : 'warning'">
                {{ currentTask.retryReady ? '可执行' : '等待中' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">下次自动重试</span>
              <span>{{ formatTime(currentTask.nextRetryTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">退避时长</span>
              <span>{{ formatDuration(currentTask.retryDelayMs) }}</span>
            </div>
          </div>

          <div v-if="currentTask.errorDetail" class="error-panel">
            <div class="error-title">失败详情</div>
            <div class="error-row">
              <span class="detail-label">错误分类</span>
              <span>{{ currentTask.errorDetail.category || 'UNKNOWN' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">错误摘要</span>
              <span>{{ currentTask.errorDetail.summary || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">错误类型</span>
              <span>{{ currentTask.errorDetail.errorType || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">错误编码</span>
              <span>{{ currentTask.errorDetail.errorCode || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">完整错误</span>
              <span class="error-message">{{ currentTask.errorDetail.message || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">最后发生时间</span>
              <span>{{ formatTime(currentTask.errorDetail.lastOccurredAt) }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">自动重试状态</span>
              <span>{{ currentTask.errorDetail.autoRetryAllowed ? '允许自动重试' : '自动重试已阻塞' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">阻塞原因</span>
              <span>{{ currentTask.errorDetail.retryBlockedReason || '-' }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">下次自动重试</span>
              <span>{{ formatTime(currentTask.errorDetail.nextRetryTime) }}</span>
            </div>
            <div class="error-row">
              <span class="detail-label">退避时长</span>
              <span>{{ formatDuration(currentTask.errorDetail.retryDelayMs) }}</span>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentTask?.errorDetail?.canRetry"
          type="primary"
          @click="retryTask(currentTask.taskId)"
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
import { ingestTaskApi } from '../api/ingestTask'

const tasks = ref([])
const tableLoading = ref(false)
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const currentTask = ref(null)
const taskStats = ref({
  total: 0,
  pending: 0,
  running: 0,
  success: 0,
  failed: 0
})
const filters = ref({
  status: '',
  kbId: ''
})
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
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

const buildQuery = () => ({
  status: filters.value.status || undefined,
  kbId: filters.value.kbId?.trim() || undefined,
  page: pagination.value.page,
  size: pagination.value.size
})

const loadTasks = async () => {
  try {
    tableLoading.value = true
    const response = await ingestTaskApi.getList(buildQuery())
    const data = response.data || {}
    tasks.value = data.records || []
    pagination.value.total = data.total || 0
  } catch (error) {
    console.error('加载任务列表失败:', error)
    ElMessage.error('加载任务列表失败')
  } finally {
    tableLoading.value = false
  }
}

const loadStats = async () => {
  const response = await ingestTaskApi.getStats({
    kbId: filters.value.kbId?.trim() || undefined
  })
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
    await Promise.all([loadTasks(), loadStats()])
  } catch (error) {
    if (showError) {
      ElMessage.error('加载任务中心数据失败')
    }
  }
}

const handleQueryChange = async () => {
  pagination.value.page = 1
  await loadPageData(false)
}

const handleSizeChange = async (size) => {
  pagination.value.size = size
  pagination.value.page = 1
  await loadTasks()
}

const showTaskDetail = async (taskId) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  currentTask.value = null
  try {
    const response = await ingestTaskApi.getDetail(taskId)
    currentTask.value = response.data
  } catch (error) {
    console.error('加载任务详情失败:', error)
    ElMessage.error('加载任务详情失败')
  } finally {
    detailLoading.value = false
  }
}

const retryTask = async (taskId) => {
  try {
    await ingestTaskApi.retry(taskId)
    ElMessage.success('任务已重新提交')
    detailDialogVisible.value = false
    await loadPageData(false)
  } catch (error) {
    console.error('任务重试失败:', error)
    ElMessage.error('任务重试失败')
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

const formatDuration = (value) => {
  if (!value && value !== 0) {
    return '-'
  }
  if (value < 1000) {
    return `${value} ms`
  }
  if (value < 60 * 1000) {
    return `${(value / 1000).toFixed(1)} s`
  }
  return `${(value / (60 * 1000)).toFixed(1)} min`
}
</script>

<style scoped>
.task-center {
  max-width: 1400px;
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.kb-input {
  width: 220px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-grid {
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
  .stats-grid,
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .stats-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .pagination {
    justify-content: flex-start;
  }
}
</style>
