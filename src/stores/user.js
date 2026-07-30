import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = () => !!token.value

  const login = (name, pwd) => {
    if (name && pwd) {
      token.value = 'mock-token-' + Date.now()
      username.value = name
      role.value = name === 'admin' ? '管理员' : '值班员'
      localStorage.setItem('token', token.value)
      localStorage.setItem('username', username.value)
      localStorage.setItem('role', role.value)
      return true
    }
    return false
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return { token, username, role, isLoggedIn, login, logout }
})
