import apiClient from './chat'

export const promptConfigApi = {
  getConfig() {
    return apiClient.get('/prompt-config')
  },

  updateConfig(data) {
    return apiClient.put('/prompt-config', data)
  },

  resetConfig() {
    return apiClient.post('/prompt-config/reset')
  }
}

export default promptConfigApi
