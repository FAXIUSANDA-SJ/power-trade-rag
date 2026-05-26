import { createRouter, createWebHistory } from 'vue-router'
import Chat from '../views/Chat.vue'
import DocumentManagement from '../views/DocumentManagement.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'
import PromptConfig from '../views/PromptConfig.vue'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Chat
  },
  {
    path: '/documents',
    name: 'DocumentManagement',
    component: DocumentManagement
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    component: KnowledgeBase
  },
  {
    path: '/prompt-config',
    name: 'PromptConfig',
    component: PromptConfig
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
