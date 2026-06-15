<template>
  <div class="dashboard">
    <header class="dash-header">
      <h1>城市地道桥积水智能联控系统</h1>
      <div class="header-info">
        <span class="time">{{ currentTime }}</span>
        <span class="alarm-count" :class="{ active: alarmCount > 0 }">
          告警: {{ alarmCount }}
        </span>
      </div>
    </header>

    <div class="dash-body">
      <aside class="sidebar-left">
        <UnderpassList
          :underpasses="underpasses"
          :selectedId="selectedId"
          @select="onSelect"
        />
      </aside>

      <main class="main-canvas">
        <CanvasMap
          :underpasses="underpasses"
          :selectedId="selectedId"
          @select="onSelect"
        />
      </main>

      <aside class="sidebar-right">
        <DetailPanel
          :underpass="selectedUnderpass"
          @lift="onManualLift"
          @lower="onManualLower"
        />
        <AlarmLog :underpasses="underpasses" />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchAllStatus, sendHydraulicCommand } from './api/underpass.js'
import { useWebSocket } from './composables/useWebSocket.js'
import UnderpassList from './components/UnderpassList.vue'
import CanvasMap from './components/CanvasMap.vue'
import DetailPanel from './components/DetailPanel.vue'
import AlarmLog from './components/AlarmLog.vue'

const underpasses = ref([])
const selectedId = ref(null)
const currentTime = ref('')

const { latestUpdates } = useWebSocket()

const alarmCount = computed(() =>
  underpasses.value.filter(u => u.status === 'ALARM').length
)

const selectedUnderpass = computed(() => {
  if (!selectedId.value) return null
  const base = underpasses.value.find(u => u.underpassId === selectedId.value)
  if (!base) return null
  const live = latestUpdates.value[selectedId.value]
  return live ? { ...base, ...live } : base
})

function onSelect(id) {
  selectedId.value = id
}

async function loadData() {
  try {
    const data = await fetchAllStatus()
    underpasses.value = data
    if (!selectedId.value && data.length > 0) {
      selectedId.value = data[0].underpassId
    }
  } catch (e) {
    console.error('Load data failed:', e)
  }
}

async function onManualLift(underpassId) {
  await sendHydraulicCommand(underpassId, 'lift', 15)
}

async function onManualLower(underpassId) {
  await sendHydraulicCommand(underpassId, 'lower', 0)
}

let timer = null
function tickTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

onMounted(() => {
  loadData()
  tickTime()
  timer = setInterval(tickTime, 1000)
  setInterval(loadData, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a1628;
  color: #e0e8f0;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.dash-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(135deg, #0d2137 0%, #162d50 100%);
  border-bottom: 1px solid rgba(64, 158, 255, 0.3);
}

.dash-header h1 {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #409eff, #67c8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-info {
  display: flex;
  gap: 20px;
  align-items: center;
  font-size: 14px;
}

.alarm-count {
  padding: 4px 12px;
  border-radius: 4px;
  background: #1a2a40;
  border: 1px solid #2a3a50;
}

.alarm-count.active {
  background: rgba(245, 63, 63, 0.2);
  border-color: #f53f3f;
  color: #f53f3f;
  animation: pulse-alarm 1s infinite;
}

@keyframes pulse-alarm {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.dash-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar-left {
  width: 260px;
  border-right: 1px solid rgba(64, 158, 255, 0.15);
  overflow-y: auto;
}

.main-canvas {
  flex: 1;
  position: relative;
}

.sidebar-right {
  width: 320px;
  border-left: 1px solid rgba(64, 158, 255, 0.15);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
</style>
