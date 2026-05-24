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
  delete(docId) {
    return apiClient.delete(`/document/${docId}`)
  }
}

export default apiClient