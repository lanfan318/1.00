<template>
<div class="login-wrapper">
  <div class="login-card">
    <div class="login-header">
      <div class="login-icon">HF</div>
      <h1>HiFire Agent</h1>
      <p>火警智能体自动检测系统</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="login-footer">
      <span>演示账号：admin / 123456</span>
    </div>
  </div>

  <div class="login-bg-text">
    <div class="bg-line">REAL-TIME MONITORING</div>
    <div class="bg-line">INTELLIGENT DIAGNOSIS</div>
    <div class="bg-line">KNOWLEDGE GRAPH</div>
  </div>
</div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  loading.value = true
  setTimeout(() => {
    const ok = userStore.login(form.username, form.password)
    loading.value = false
    if (ok) {
      ElMessage.success('登录成功，欢迎使用 HiFire Agent')
      router.push('/dashboard')
    } else {
      ElMessage.error('用户名或密码不能为空')
    }
  }, 600)
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  background: radial-gradient(ellipse at 50% 50%, #0f1629 0%, #050810 70%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-card {
  width: 420px;
  background: rgba(17, 24, 39, 0.85);
  border: 0.5px solid rgba(59, 130, 246, 0.3);
  border-radius: 16px;
  padding: 48px 40px 36px;
  backdrop-filter: blur(12px);
  position: relative;
  z-index: 2;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 4px 24px rgba(59, 130, 246, 0.3);
}

.login-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #e2e8f0;
  margin: 0 0 6px;
}

.login-header h1 span {
  color: #3b82f6;
}

.login-header p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-input__wrapper) {
  background: #0a0e17;
  border-color: #1e293b;
  box-shadow: none;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #2a3544;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3);
}

.login-form :deep(.el-input__inner) {
  color: #e2e8f0;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #64748b;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
}

.login-btn:hover {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #475569;
}

.login-bg-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
  opacity: 0.04;
  text-align: center;
}

.bg-line {
  font-size: 48px;
  font-weight: 800;
  color: #3b82f6;
  letter-spacing: 12px;
  white-space: nowrap;
  line-height: 1.4;
}

@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 36px 24px 28px;
  }
  .bg-line {
    font-size: 28px;
  }
}
</style>
