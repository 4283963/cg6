<template>
  <div class="alarm-log">
    <div class="log-title">告警记录</div>
    <div class="log-list">
      <div
        v-for="u in alarmUnderpasses"
        :key="u.underpassId"
        class="log-item"
      >
        <span class="log-dot"></span>
        <span class="log-name">{{ u.name || u.underpassId }}</span>
        <span class="log-depth">{{ (u.currentDepthMm || 0).toFixed(0) }}mm</span>
      </div>
      <div v-if="alarmUnderpasses.length === 0" class="log-empty">
        暂无告警
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  underpasses: { type: Array, default: () => [] }
})

const alarmUnderpasses = computed(() =>
  props.underpasses.filter(u => u.status === 'ALARM')
)
</script>

<style scoped>
.alarm-log {
  padding: 16px;
  border-top: 1px solid rgba(64, 158, 255, 0.15);
}

.log-title {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12px;
}

.log-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f53f3f;
  animation: dot-blink 0.8s infinite;
}

@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.log-name {
  flex: 1;
  color: #e0e8f0;
}

.log-depth {
  color: #f53f3f;
  font-weight: 600;
  font-family: "Roboto Mono", monospace;
}

.log-empty {
  text-align: center;
  color: #5a6a7a;
  font-size: 12px;
  padding: 10px 0;
}
</style>
