import apiClient from './chat'

export const ingestTaskApi = {
  getList(params) {
    return apiClient.get('/ingest-task/list', { params })
  },
  getStats(params) {
    return apiClient.get('/ingest-task/stats', { params })
  },
  getDetail(taskId) {
    return apiClient.get(`/ingest-task/${taskId}`)
  },
  retry(taskId) {
    return apiClient.post(`/ingest-task/${taskId}/retry`)
  }
}

export default ingestTaskApi
