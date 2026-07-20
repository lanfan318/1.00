<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useDataStore } from '@/stores/data'

const store = useDataStore()
onMounted(() => {
  store.init()
})
</script>

<style>
html, body, #app { height: 100%; margin: 0; padding: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #0a0e17;
  color: #e2e8f0;
}
</style>
