<!--
  HudPanel.vue — 火警监控 HUD 科技面板组件
  基于原生 CSS 设计系统（详见 src/styles/hud.css），封装数据卡片 + 标题栏 + 角落装饰。
  用法：
    <HudPanel title="防护分区状态" variant="alarm"> ...内容... </HudPanel>
  全局类（外框 / 分割线 / 角落 / 状态点）请直接从 hud.css 复制使用。
-->
<template>
  <div class="hp" :class="[variant && `hp--${variant}`]">
    <div v-if="title" class="hp-bar">
      <span class="hp-bar-tx">{{ title }}</span>
      <slot name="bar-extra" />
    </div>
    <div class="hp-body">
      <slot />
    </div>
    <i class="hp-corner hp-corner--tr"></i>
  </div>
</template>

<script setup>
defineProps({
  title:   { type: String, default: '' },      // 标题栏文字，留空则不显示标题栏
  variant: { type: String, default: '' }       // '' | 'alarm' | 'warn'
})
</script>

<style scoped>
.hp {
  position: relative;
  background: rgba(0, 25, 45, 0.45);
  border: 1px solid rgba(30, 74, 112, 0.8);
  border-radius: 4px;
  padding: 14px;
  box-shadow: inset 0 0 12px rgba(0, 160, 255, 0.08);
}
.hp--alarm { border-color: rgba(224, 122, 107, 0.5);  box-shadow: inset 0 0 14px rgba(224, 122, 107, 0.15); }
.hp--warn  { border-color: rgba(224, 168, 92, 0.5);   box-shadow: inset 0 0 14px rgba(224, 168, 92, 0.13); }

.hp-bar {
  height: 34px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  margin: -14px -14px 12px;
  background: rgba(0, 30, 55, 0.45);
  border-bottom: 1px solid rgba(30, 74, 112, 0.8);
  color: #cfe3f5;
  letter-spacing: 1px;
  font-size: 13px;
  position: relative;
}
.hp-bar::before {
  content: "";
  position: absolute; left: 0; top: 0;
  width: 10px; height: 100%;
  background: linear-gradient(90deg, rgba(0, 170, 255, 0.35), transparent);
  clip-path: polygon(0 0, 100% 25%, 100% 75%, 0 100%);
}
.hp-bar-tx { flex: 1; }
.hp-bar :slotted(*) { margin-left: auto; }

.hp-body { position: relative; }

/* 右上角斜纹装饰 */
.hp-corner {
  position: absolute;
  top: 0; right: 0;
  width: 22px; height: 22px;
  pointer-events: none;
  border-top: 2px solid rgba(0, 170, 255, 0.35);
  border-right: 2px solid rgba(0, 170, 255, 0.35);
  background: linear-gradient(-45deg, transparent 62%, rgba(0, 170, 255, 0.18) 62%);
}
.hp--alarm .hp-corner { border-color: #e07a6b; background: linear-gradient(-45deg, transparent 62%, rgba(224,122,107,0.22) 62%); }
.hp--warn  .hp-corner { border-color: #e0a85c; background: linear-gradient(-45deg, transparent 62%, rgba(224,168,92,0.22) 62%); }
</style>
