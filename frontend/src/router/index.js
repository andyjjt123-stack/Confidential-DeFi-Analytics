import { createRouter, createWebHistory } from 'vue-router'
import VaultView from '../views/VaultView.vue'
export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [{ path: '/', name: 'vault', component: VaultView }]
})
