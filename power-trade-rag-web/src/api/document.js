import apiClient from './chat'

export const documentApi = {
  upload(formData) {
    return apiClient.post('/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  getList(params) {
    return apiClient.get('/document/list', { params })
  },
  testOcr(formData) {
    return apiClient.post('/document/ocr/test', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  getTaskStats(params) {
    return apiClient.get('/ingest-task/stats', { params })
  },
  getTaskDetail(taskId) {
    return apiClient.get(`/ingest-task/${taskId}`)
  },
  retryTask(taskId) {
    return apiClient.post(`/ingest-task/${taskId}/retry`)
  },
  delete(docId) {
    return apiClient.delete(`/document/${docId}`)
  }
}

export default apiClient
