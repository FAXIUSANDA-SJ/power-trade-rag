import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const chatApi = {
  ask(data) {
    return apiClient.post('/chat/ask', data)
  },

  clearSession(sessionId) {
    return apiClient.delete(`/chat/session/${sessionId}`)
  }
}

export default apiClient
