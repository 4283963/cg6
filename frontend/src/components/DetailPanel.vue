<template>
  <div class="detail-panel" v-if="underpass">
    <div class="panel-title">监测详情</div>

    <div class="info-grid">
      <div class="info-label">编号</div>
      <div class="info-value">{{ underpass.underpassId }}</div>

      <div class="info-label">名称</div>
      <div class="info-value">{{ underpass.name || '-' }}</div>

      <div class="info-label">当前积水</div>
      <div class="info-value depth" :class="{ danger: (underpass.currentDepthMm || 0) >= 100 }">
        {{ (underpass.currentDepthMm || 0).toFixed(1) }} mm
        <span class="depth-tag filtered">滤波</span>
        <span class="depth-bar">
          <span
            class="depth-fill"
            :style="{ width: Math.min(100, (underpass.currentDepthMm || 0) / 2) + '%' }"
            :class="{ danger: (underpass.currentDepthMm || 0) >= 100 }"
          ></span>
        </span>
      </div>

      <div class="info-label">原始积水</div>
      <div class="info-value raw-depth">
        {{ (underpass.rawDepthMm || 0).toFixed(1) }} mm
        <span class="depth-tag raw">原始</span>
        <span
          v-if="underpass.rawDepthMm && underpass.currentDepthMm && Math.abs(underpass.rawDepthMm - underpass.currentDepthMm) > 15"
          class="spike-warning"
        >⚠ 毛刺</span>
      </div>

      <div class="info-label">液压井盖</div>
      <div class="info-value">
        <span class="hydraulic-state" :class="hydraulicStateClass">
          {{ hydraulicStateText }}
        </span>
      </div>

      <div class="info-label">LED警示灯</div>
      <div class="info-value">
        <span v-if="underpass.status === 'ALARM'" class="led-state led-alarm">红色 · 积水危险</span>
        <span v-else-if="underpass.forecastActive" class="led-state led-forecast">黄色 · 预备排水</span>
        <span v-else class="led-state led-normal">绿色 · 正常通行</span>
      </div>

      <div class="info-label">天气</div>
      <div class="info-value">
        <span v-if="underpass.currentlyRaining" class="weather rain">🌧 下雨</span>
        <span v-else-if="underpass.currentlyRaining === false" class="weather clear">☀ 晴</span>
        <span v-else class="weather unknown">— 未采集</span>
      </div>

      <div class="info-label">状态</div>
      <div class="info-value">
        <span class="status-badge" :class="statusClass">
          {{ statusText }}
        </span>
      </div>
    </div>

    <div class="forecast-section" v-if="underpass.upstreamCatchmentId">
      <div class="section-title">
        <span>🔮 上游雨情前瞻</span>
        <span v-if="underpass.forecastActive" class="forecast-tag">已触发</span>
      </div>
      <div class="forecast-info">
        <div class="forecast-row">
          <span>关联汇水区</span>
          <span class="forecast-val">{{ underpass.upstreamCatchmentName || underpass.upstreamCatchmentId }}</span>
        </div>
        <div class="forecast-row">
          <span>当前流量</span>
          <span class="forecast-val">{{ (underpass.lastFlowRateLps || 0).toFixed(1) }} L/s</span>
        </div>
        <div class="forecast-row">
          <span>10分钟前</span>
          <span class="forecast-val">{{ (underpass.flowRate10MinAgoLps || 0).toFixed(1) }} L/s</span>
        </div>
        <div class="forecast-row">
          <span>流量倍数</span>
          <span class="forecast-val" :class="{ danger: surgeRatio >= 3 }">
            {{ surgeRatio.toFixed(2) }}x
          </span>
        </div>
      </div>
    </div>

    <div class="link-section">
      <div class="section-title">🔗 关联上游汇水区</div>
      <select v-model="selectedUpstreamId" @change="onLinkChange" class="link-select">
        <option value="">— 不关联 —</option>
        <option v-for="c in catchments" :key="c.id" :value="c.id">
          {{ c.name }} ({{ c.id }})
        </option>
      </select>
    </div>

    <div class="panel-actions">
      <button class="btn btn-prelift" @click="$emit('prelift', underpass.underpassId)">
        ⬆ 半顶升5cm
      </button>
      <button class="btn btn-lift" @click="$emit('lift', underpass.underpassId)">
        ⬆ 全顶升15cm
      </button>
      <button class="btn btn-lower" @click="$emit('lower', underpass.underpassId)">
        ⬇ 降下
      </button>
    </div>
  </div>

  <div class="detail-panel empty" v-else>
    <div class="panel-title">监测详情</div>
    <div class="empty-text">请选择一个地道桥监测点</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { fetchUpstreamList, linkUpstreamCatchment } from '../api/underpass.js'

const props = defineProps({
  underpass: { type: Object, default: null }
})
const emit = defineEmits(['lift', 'lower', 'prelift', 'linkChanged'])

const catchments = ref([])
const selectedUpstreamId = ref('')

async function loadCatchments() {
  catchments.value = await fetchUpstreamList()
}

watch(() => props.underpass?.upstreamCatchmentId, (newVal) => {
  selectedUpstreamId.value = newVal || ''
}, { immediate: true })

async function onLinkChange() {
  if (!props.underpass) return
  try {
    await linkUpstreamCatchment(props.underpass.underpassId, selectedUpstreamId.value)
    emit('linkChanged')
  } catch (e) {
    alert('关联失败：' + e.message)
  }
}

const surgeRatio = computed(() => {
  const last = props.underpass?.lastFlowRateLps || 0
  const ago = props.underpass?.flowRate10MinAgoLps || 0
  if (ago <= 0) return 0
  return last / ago
})

const hydraulicStateText = computed(() => {
  const s = props.underpass?.hydraulicState
  if (s === 'PRE_LIFTED') return '已半顶升 5cm'
  if (s === 'FULL_LIFTED') return '已全顶升 15cm'
  return '已降下'
})

const hydraulicStateClass = computed(() => {
  const s = props.underpass?.hydraulicState
  if (s === 'PRE_LIFTED') return 'state-pre'
  if (s === 'FULL_LIFTED') return 'state-full'
  return 'state-down'
})

const statusText = computed(() => {
  if (props.underpass?.status === 'ALARM') return '告警'
  if (props.underpass?.forecastActive) return '前瞻预警'
  return '正常'
})

const statusClass = computed(() => {
  if (props.underpass?.status === 'ALARM') return 'badge-alarm'
  if (props.underpass?.forecastActive) return 'badge-forecast'
  return 'badge-normal'
})

onMounted(loadCatchments)
defineExpose({ loadCatchments })
</script>

<style scoped>
.detail-panel {
  padding: 16px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.2);
}

.info-grid {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px 12px;
  margin-bottom: 16px;
}

.info-label {
  font-size: 12px;
  color: #86909c;
  align-self: center;
}

.info-value {
  font-size: 13px;
  color: #e0e8f0;
}

.info-value.depth {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-value.depth.danger {
  color: #f53f3f;
  font-weight: 700;
}

.raw-depth {
  color: #86909c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.depth-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.depth-tag.filtered {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.3);
}

.depth-tag.raw {
  background: rgba(134, 144, 156, 0.15);
  color: #86909c;
  border: 1px solid rgba(134, 144, 156, 0.3);
}

.spike-warning {
  color: #ff7d00;
  font-size: 11px;
  font-weight: 600;
  animation: spike-blink 0.6s infinite;
}

@keyframes spike-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.depth-bar {
  flex: 1;
  height: 6px;
  background: #1a2a40;
  border-radius: 3px;
  overflow: hidden;
}

.depth-fill {
  display: block;
  height: 100%;
  background: #409eff;
  border-radius: 3px;
  transition: width 0.5s;
}

.depth-fill.danger {
  background: #f53f3f;
}

.hydraulic-state {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.state-down {
  background: rgba(0, 180, 42, 0.15);
  color: #00b42a;
  border: 1px solid rgba(0, 180, 42, 0.3);
}

.state-pre {
  background: rgba(255, 125, 0, 0.15);
  color: #ff7d00;
  border: 1px solid rgba(255, 125, 0, 0.3);
}

.state-full {
  background: rgba(245, 63, 63, 0.15);
  color: #f53f3f;
  border: 1px solid rgba(245, 63, 63, 0.3);
}

.led-state {
  font-size: 12px;
  font-weight: 500;
}

.led-normal { color: #00b42a; }
.led-alarm { color: #f53f3f; animation: blink 0.8s infinite; }
.led-forecast { color: #ff7d00; animation: blink 1.5s infinite; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.weather {
  font-size: 12px;
}
.weather.rain { color: #409eff; }
.weather.clear { color: #ffd21e; }
.weather.unknown { color: #5a6a7a; }

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.badge-normal {
  background: rgba(0, 180, 42, 0.15);
  color: #00b42a;
  border: 1px solid rgba(0, 180, 42, 0.3);
}

.badge-alarm {
  background: rgba(245, 63, 63, 0.15);
  color: #f53f3f;
  border: 1px solid rgba(245, 63, 63, 0.3);
  animation: badge-blink 1s infinite;
}

.badge-forecast {
  background: rgba(255, 125, 0, 0.15);
  color: #ff7d00;
  border: 1px solid rgba(255, 125, 0, 0.3);
  animation: badge-blink 1.5s infinite;
}

@keyframes badge-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #e0e8f0;
  margin: 12px 0 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forecast-tag {
  background: rgba(255, 125, 0, 0.2);
  color: #ff7d00;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 4px;
  animation: tag-blink 1.2s infinite;
}

@keyframes tag-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.forecast-info {
  background: #0f1f35;
  border: 1px solid rgba(255, 125, 0, 0.2);
  border-radius: 6px;
  padding: 10px;
}

.forecast-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.05);
}

.forecast-row:last-child { border-bottom: none; }

.forecast-row > span:first-child {
  color: #86909c;
}

.forecast-val {
  color: #e0e8f0;
  font-family: "Roboto Mono", monospace;
}

.forecast-val.danger {
  color: #f53f3f;
  font-weight: 600;
}

.link-select {
  width: 100%;
  background: #0a1628;
  border: 1px solid rgba(64, 158, 255, 0.3);
  border-radius: 4px;
  padding: 6px 10px;
  color: #e0e8f0;
  font-size: 12px;
}

.panel-actions {
  display: flex;
  gap: 6px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.btn {
  flex: 1;
  min-width: 80px;
  padding: 7px 0;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-prelift {
  background: rgba(255, 125, 0, 0.15);
  color: #ff7d00;
  border: 1px solid rgba(255, 125, 0, 0.3);
}

.btn-prelift:hover {
  background: rgba(255, 125, 0, 0.25);
}

.btn-lift {
  background: rgba(245, 63, 63, 0.15);
  color: #f53f3f;
  border: 1px solid rgba(245, 63, 63, 0.3);
}

.btn-lift:hover {
  background: rgba(245, 63, 63, 0.25);
}

.btn-lower {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.3);
}

.btn-lower:hover {
  background: rgba(64, 158, 255, 0.25);
}

.empty-text {
  text-align: center;
  color: #5a6a7a;
  font-size: 13px;
  padding: 30px 0;
}
</style>
