<template>
<div class="login-wrapper">
  <!-- 背景网格 + 光晕 -->
  <div class="lg-grid"></div>
  <div class="lg-glow lg-glow-1"></div>
  <div class="lg-glow lg-glow-2"></div>
  <div class="lg-scan"></div>
  <div class="lg-frame">
    <span class="fc tl"></span><span class="fc tr"></span><span class="fc bl"></span><span class="fc br"></span>
  </div>

  <div class="lg-shell">
    <!-- 左侧品牌面板 -->
    <div class="lg-brand">
      <div class="lg-brand-inner">
        <div class="lg-logo">
          <div class="lg-hex">HF</div>
          <div class="lg-name-wrap">
            <span class="lg-name">HiFire Agent</span>
            <span class="lg-tag">火电智能预警与故障诊断系统</span>
          </div>
        </div>

        <h1 class="lg-title">智能预警 · 故障诊断<br/>一体化运行平台</h1>
        <p class="lg-desc">基于实时运行数据与设备知识图谱，提供监盘预警、故障溯源、工况分析与 AI 智能诊断能力。</p>

        <div class="lg-feats">
          <div class="lg-feat"><span class="lf-ic">◈</span><div><div class="lf-t">全域实时告警</div><div class="lf-d">毫秒级监测，分级推送</div></div></div>
          <div class="lg-feat"><span class="lf-ic">⬡</span><div><div class="lf-t">知识图谱推理</div><div class="lf-d">可追溯的故障链路</div></div></div>
          <div class="lg-feat"><span class="lf-ic">⚡</span><div><div class="lf-t">AI 运行智能体</div><div class="lf-d">自然语言交互诊断</div></div></div>
        </div>

        <div class="lg-foot">Power Plant Intelligent Early Warning System</div>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="lg-form-area">
      <div class="login-card">
        <div class="lc-corner lc-tl"></div><div class="lc-corner lc-tr"></div>
        <div class="lc-corner lc-bl"></div><div class="lc-corner lc-br"></div>

        <div class="lc-h">账号登录</div>
        <div class="lc-sub-h">请输入管理员账号以进入系统</div>

        <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="lc-btn">
              登 录 系 统
            </el-button>
          </el-form-item>
        </el-form>

        <div class="lc-footer">
          <span>默认账号: admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh; position: relative; overflow: hidden;
  background:
    radial-gradient(ellipse 900px 500px at 20% 10%, rgba(15,55,100,0.18), transparent 60%),
    radial-gradient(ellipse 700px 400px at 85% 90%, rgba(10,70,120,0.12), transparent 55%),
    linear-gradient(180deg, #0a1220 0%, #0c1829 40%, #0a1424 100%);
}
.lg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(45,80,115,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45,80,115,0.04) 1px, transparent 1px);
  background-size: 48px 48px;
}
.lg-glow { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.5; pointer-events: none; }
.lg-glow-1 { width: 420px; height: 420px; background: rgba(40,120,200,0.25); top: -120px; left: -80px; }
.lg-glow-2 { width: 360px; height: 360px; background: rgba(20,160,180,0.18); bottom: -100px; right: -60px; }
.lg-scan { position: absolute; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, transparent, rgba(62,170,255,0.5), transparent); opacity: 0.3; animation: scanMove 6s linear infinite; }
@keyframes scanMove { 0% { top: 0; } 100% { top: 100%; } }

.lg-frame { position: absolute; inset: 0; pointer-events: none; }
.fc { position: absolute; width: 18px; height: 18px; border: 2px solid rgba(62,170,255,0.6); box-shadow: 0 0 10px rgba(62,170,255,0.4); }
.fc.tl { top: 16px; left: 16px; border-right: none; border-bottom: none; }
.fc.tr { top: 16px; right: 16px; border-left: none; border-bottom: none; }
.fc.bl { bottom: 16px; left: 16px; border-right: none; border-top: none; }
.fc.br { bottom: 16px; right: 16px; border-left: none; border-top: none; }

.lg-shell { position: relative; z-index: 2; min-height: 100vh; display: grid; grid-template-columns: 1.15fr 1fr; }

/* 品牌面板 */
.lg-brand { display: flex; align-items: center; padding: 0 6vw; }
.lg-brand-inner { max-width: 520px; width: 100%; display: flex; flex-direction: column; justify-content: center; min-height: 60vh; }
.lg-logo { display: flex; align-items: center; gap: 16px; margin-bottom: 36px; }
.lg-hex {
  width: 56px; height: 62px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: linear-gradient(135deg, rgba(62,170,255,0.7) 0%, rgba(30,100,180,0.5) 100%);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800; color: #fff; letter-spacing: -1px;
  box-shadow: 0 0 20px rgba(62,170,255,0.4);
}
.lg-name-wrap { display: flex; flex-direction: column; gap: 3px; }
.lg-name { font-size: 22px; font-weight: 800; color: #e8f0f8; letter-spacing: 1px; }
.lg-tag { font-size: 12px; color: #8fb0cf; letter-spacing: 0.5px; }
.lg-title { font-size: 38px; font-weight: 800; line-height: 1.25; color: #eaf4ff; letter-spacing: 1px; margin-bottom: 18px; text-shadow: 0 0 30px rgba(62,170,255,0.25); }
.lg-desc { font-size: 14px; color: #8fb0cf; line-height: 1.9; margin-bottom: 36px; max-width: 460px; }
.lg-feats { display: flex; flex-direction: column; gap: 18px; }
.lg-feat { display: flex; align-items: center; gap: 14px; }
.lf-ic { width: 40px; height: 40px; flex-shrink: 0; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #7ec8ff; background: rgba(62,170,255,0.1); border: 1px solid rgba(62,170,255,0.25); box-shadow: 0 0 14px rgba(62,170,255,0.12); }
.lf-t { font-size: 15px; font-weight: 600; color: #d4ecff; }
.lf-d { font-size: 12px; color: #8fb0cf; margin-top: 2px; }
.lg-foot { margin-top: 46px; font-size: 11px; color: #9fb6cf; letter-spacing: 1px; }

/* 登录卡片 */
.lg-form-area { display: flex; align-items: center; justify-content: center; padding: 40px; }
.login-card {
  width: 380px; position: relative; z-index: 2;
  background: linear-gradient(180deg, rgba(12,26,46,0.82), rgba(8,18,34,0.88));
  border: 1px solid rgba(62,170,255,0.22); border-radius: 8px;
  padding: 40px 36px 30px;
  box-shadow: 0 12px 48px rgba(0,0,0,0.45), inset 0 1px 0 rgba(62,170,255,0.08);
  backdrop-filter: blur(6px);
}
.lc-corner { position: absolute; width: 14px; height: 14px; border: 1.5px solid #3eaaff; box-shadow: 0 0 8px rgba(62,170,255,0.45); }
.lc-tl { top: -1px; left: -1px; border-right: none; border-bottom: none; }
.lc-tr { top: -1px; right: -1px; border-left: none; border-bottom: none; }
.lc-bl { bottom: -1px; left: -1px; border-right: none; border-top: none; }
.lc-br { bottom: -1px; right: -1px; border-left: none; border-top: none; }
.lc-h { font-size: 22px; font-weight: 700; color: #eaf4ff; letter-spacing: 1px; text-align: center; }
.lc-sub-h { font-size: 12px; color: #8fb0cf; text-align: center; margin: 8px 0 28px; }

.lc-btn { width: 100%; height: 44px; font-size: 15px; font-weight: 600; letter-spacing: 4px; border-radius: 5px;
  background: linear-gradient(135deg, rgba(45,120,190,0.9), rgba(35,100,165,0.9)) !important;
  border: 1px solid rgba(62,150,220,0.4) !important; box-shadow: 0 4px 20px rgba(50,130,200,0.3); transition: all 0.25s; }
.lc-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 28px rgba(50,130,200,0.45); }

.lc-footer { text-align: center; margin-top: 18px; font-size: 11px; color: #9fb6cf; }

@media (max-width: 980px) {
  .lg-shell { grid-template-columns: 1fr; }
  .lg-brand { display: none; }
}
</style>
