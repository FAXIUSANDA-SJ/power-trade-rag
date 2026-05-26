<template>
  <div class="prompt-config-container">
    <el-card class="config-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>提示词配置</h2>
            <p>配置客服助手的人设、欢迎语、系统提示词和记忆轮数。</p>
          </div>
          <div class="header-actions">
            <el-button @click="loadConfig" :loading="loading">刷新</el-button>
            <el-button @click="handleReset" :loading="saving">恢复默认</el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
          </div>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="config-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="助手名称" prop="assistantName">
              <el-input v-model="form.assistantName" maxlength="20" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记忆轮数" prop="memoryRounds">
              <el-input-number v-model="form.memoryRounds" :min="1" :max="20" :step="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="欢迎语" prop="welcomeMessage">
          <el-input
            v-model="form.welcomeMessage"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="12"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="兜底回复" prop="fallbackReply">
          <el-input
            v-model="form.fallbackReply"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div class="config-meta">
        <span>最近更新时间：{{ form.updatedAt || '未保存' }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { promptConfigApi } from '../api/promptConfig'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  assistantName: '',
  welcomeMessage: '',
  systemPrompt: '',
  fallbackReply: '',
  memoryRounds: 6,
  updatedAt: ''
})

const rules = {
  assistantName: [
    { required: true, message: '请输入助手名称', trigger: 'blur' }
  ],
  welcomeMessage: [
    { required: true, message: '请输入欢迎语', trigger: 'blur' }
  ],
  systemPrompt: [
    { required: true, message: '请输入系统提示词', trigger: 'blur' }
  ],
  fallbackReply: [
    { required: true, message: '请输入兜底回复', trigger: 'blur' }
  ]
}

const applyFormData = (data = {}) => {
  form.assistantName = data.assistantName || ''
  form.welcomeMessage = data.welcomeMessage || ''
  form.systemPrompt = data.systemPrompt || ''
  form.fallbackReply = data.fallbackReply || ''
  form.memoryRounds = data.memoryRounds || 6
  form.updatedAt = data.updatedAt || ''
}

const loadConfig = async () => {
  loading.value = true
  try {
    const { data } = await promptConfigApi.getConfig()
    applyFormData(data)
  } catch (error) {
    console.error('加载提示词配置失败:', error)
    ElMessage.error('加载提示词配置失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  saving.value = true
  try {
    const payload = {
      assistantName: form.assistantName,
      welcomeMessage: form.welcomeMessage,
      systemPrompt: form.systemPrompt,
      fallbackReply: form.fallbackReply,
      memoryRounds: form.memoryRounds
    }

    const { data } = await promptConfigApi.updateConfig(payload)
    applyFormData(data)
    ElMessage.success('提示词配置已保存')
  } catch (error) {
    console.error('保存提示词配置失败:', error)
    ElMessage.error('保存提示词配置失败')
  } finally {
    saving.value = false
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm('确认恢复默认提示词配置吗？', '提示', {
      type: 'warning'
    })
  } catch (error) {
    return
  }

  saving.value = true
  try {
    const { data } = await promptConfigApi.resetConfig()
    applyFormData(data)
    ElMessage.success('已恢复默认配置')
  } catch (error) {
    console.error('恢复默认配置失败:', error)
    ElMessage.error('恢复默认配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.prompt-config-container {
  padding: 20px;
}

.config-card {
  max-width: 1100px;
  margin: 0 auto;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}

.card-header p {
  margin: 0;
  color: #606266;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.config-form {
  margin-top: 12px;
}

.config-meta {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
