<template>
  <div class="underpass-list">
    <div class="list-title">地道桥监测点</div>
    <div
      v-for="u in underpasses"
      :key="u.underpassId"
      class="list-item"
      :class="{
        selected: u.underpassId === selectedId,
        alarm: u.status === 'ALARM',
        forecast: u.status !== 'ALARM' && u.forecastActive
      }"
      @click="$emit('select', u.underpassId)"
    >
      <div class="item-header">
        <span class="dot" :class="dotClass(u)"></span>
        <span class="name">{{ u.name || u.underpassId }}</span>
      </div>
      <div class="item-depth">
        <span class="depth-val" :class="{ danger: (u.currentDepthMm || 0) >= 100 }">
          {{ (u.currentDepthMm || 0).toFixed(0) }}
        </span>
        <span class="depth-unit">mm</span>
      </div>
      <div class="item-status">
        {{ getStatusText(u) }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  underpasses: { type: Array, default: () => [] },
  selectedId: { type: String, default: null }
})
defineEmits(['select'])

function dotClass(u) {
  if (u.status === 'ALARM') return 'dot-alarm'
  if (u.forecastActive) return 'dot-forecast'
  return 'dot-normal'
}

function getStatusText(u) {
  if (u.status === 'ALARM') return '⚠ 告警'
  if (u.forecastActive) return '🔮 前瞻预警'
  return '● 正常'
}
</script>

<style scoped>
.underpass-list {
  padding: 12px;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.2);
}

.list-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 6px;
  background: #0f1f35;
  border: 1px solid rgba(64, 158, 255, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}

.list-item:hover {
  background: #142840;
  border-color: rgba(64, 158, 255, 0.3);
}

.list-item.selected {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.08);
}

.list-item.alarm {
  border-color: rgba(245, 63, 63, 0.5);
  animation: item-alarm-pulse 1.5s infinite;
}

@keyframes item-alarm-pulse {
  0%, 100% { background: rgba(245, 63, 63, 0.05); }
  50% { background: rgba(245, 63, 63, 0.15); }
}

.list-item.forecast {
  border-color: rgba(255, 125, 0, 0.5);
  animation: item-forecast-pulse 2s infinite;
}

@keyframes item-forecast-pulse {
  0%, 100% { background: rgba(255, 125, 0, 0.05); }
  50% { background: rgba(255, 125, 0, 0.15); }
}

.item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-normal { background: #00b42a; }
.dot-alarm { background: #f53f3f; animation: dot-blink 0.8s infinite; }
.dot-forecast { background: #ff7d00; animation: dot-forecast-blink 1.2s infinite; }

@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@keyframes dot-forecast-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.name {
  font-size: 13px;
  font-weight: 500;
}

.item-depth {
  margin: 4px 0;
}

.depth-val {
  font-size: 22px;
  font-weight: 700;
  font-family: "DIN Alternate", "Roboto Mono", monospace;
  color: #e0e8f0;
}

.depth-val.danger {
  color: #f53f3f;
}

.depth-unit {
  font-size: 12px;
  color: #86909c;
  margin-left: 2px;
}

.item-status {
  font-size: 12px;
  color: #86909c;
}
</style>
