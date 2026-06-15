<template>
  <canvas ref="canvasRef" class="canvas-map"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  underpasses: { type: Array, default: () => [] },
  selectedId: { type: String, default: null }
})
const emit = defineEmits(['select'])

const canvasRef = ref(null)
let animFrame = null
let animTime = 0

const UNDERPASS_POSITIONS = [
  { id: 'UP-BRIDGE-01', x: 0.25, y: 0.35, name: '建设路地道桥' },
  { id: 'UP-BRIDGE-02', x: 0.55, y: 0.55, name: '人民路地道桥' },
  { id: 'UP-BRIDGE-03', x: 0.75, y: 0.30, name: '解放路地道桥' },
  { id: 'UP-BRIDGE-04', x: 0.40, y: 0.75, name: '中山路地道桥' },
  { id: 'UP-BRIDGE-05', x: 0.15, y: 0.65, name: '光明路地道桥' }
]

function draw(ctx, w, h, time) {
  ctx.clearRect(0, 0, w, h)

  drawBackground(ctx, w, h)
  drawRoads(ctx, w, h)

  for (const pos of UNDERPASS_POSITIONS) {
    const up = props.underpasses.find(u => u.underpassId === pos.id)
    const isAlarm = up && up.status === 'ALARM'
    const isForecast = up && up.status !== 'ALARM' && up.forecastActive
    const isSelected = pos.id === props.selectedId
    drawUnderpassNode(ctx, w, h, pos, up, isAlarm, isForecast, isSelected, time)
  }
}

function drawBackground(ctx, w, h) {
  const grad = ctx.createLinearGradient(0, 0, 0, h)
  grad.addColorStop(0, '#0d1e36')
  grad.addColorStop(1, '#0a1628')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, w, h)

  ctx.strokeStyle = 'rgba(64, 158, 255, 0.05)'
  ctx.lineWidth = 1
  const gridSize = 50
  for (let x = 0; x < w; x += gridSize) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, h)
    ctx.stroke()
  }
  for (let y = 0; y < h; y += gridSize) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(w, y)
    ctx.stroke()
  }
}

function drawRoads(ctx, w, h) {
  ctx.strokeStyle = 'rgba(64, 158, 255, 0.12)'
  ctx.lineWidth = 3

  const roads = [
    [[0.05, 0.35], [0.95, 0.35]],
    [[0.05, 0.55], [0.95, 0.55]],
    [[0.25, 0.05], [0.25, 0.95]],
    [[0.55, 0.05], [0.55, 0.95]],
    [[0.75, 0.05], [0.75, 0.95]],
    [[0.40, 0.05], [0.40, 0.95]],
    [[0.05, 0.75], [0.95, 0.75]],
    [[0.15, 0.05], [0.15, 0.95]],
  ]

  for (const [start, end] of roads) {
    ctx.beginPath()
    ctx.moveTo(start[0] * w, start[1] * h)
    ctx.lineTo(end[0] * w, end[1] * h)
    ctx.stroke()
  }
}

function drawUnderpassNode(ctx, w, h, pos, data, isAlarm, isForecast, isSelected, time) {
  const cx = pos.x * w
  const cy = pos.y * h
  const baseR = 28

  if (isAlarm || isForecast) {
    const color = isAlarm ? '245, 63, 63' : '255, 125, 0'
    const pulseSpeed = isAlarm ? 0.004 : 0.003
    const pulse = Math.sin(time * pulseSpeed) * 0.5 + 0.5
    const glowR = baseR + 20 + pulse * 15
    const grad = ctx.createRadialGradient(cx, cy, baseR, cx, cy, glowR)
    grad.addColorStop(0, `rgba(${color}, 0.6)`)
    grad.addColorStop(0.5, `rgba(${color}, 0.2)`)
    grad.addColorStop(1, `rgba(${color}, 0)`)
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(cx, cy, glowR, 0, Math.PI * 2)
    ctx.fill()

    const ringR = baseR + 8 + pulse * 8
    ctx.strokeStyle = `rgba(${color}, ${0.3 + pulse * 0.5})`
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.arc(cx, cy, ringR, 0, Math.PI * 2)
    ctx.stroke()
  }

  if (isSelected) {
    ctx.strokeStyle = '#409eff'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.arc(cx, cy, baseR + 6, 0, Math.PI * 2)
    ctx.stroke()
  }

  ctx.beginPath()
  ctx.arc(cx, cy, baseR, 0, Math.PI * 2)
  if (isAlarm) {
    const pulse = Math.sin(time * 0.005) * 0.3 + 0.7
    ctx.fillStyle = `rgba(245, 63, 63, ${pulse})`
  } else if (isForecast) {
    const pulse = Math.sin(time * 0.004) * 0.3 + 0.7
    ctx.fillStyle = `rgba(255, 125, 0, ${pulse})`
  } else {
    ctx.fillStyle = '#1a3050'
  }
  ctx.fill()
  ctx.strokeStyle = isAlarm ? '#f53f3f' : (isForecast ? '#ff7d00' : '#409eff')
  ctx.lineWidth = 2
  ctx.stroke()

  const depth = data ? (data.currentDepthMm || 0) : 0
  ctx.fillStyle = (isAlarm || isForecast) ? '#fff' : '#e0e8f0'
  ctx.font = 'bold 14px "Roboto Mono", monospace'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(depth.toFixed(0), cx, cy - 4)
  ctx.font = '10px sans-serif'
  ctx.fillStyle = '#86909c'
  ctx.fillText('mm', cx, cy + 12)

  ctx.fillStyle = '#e0e8f0'
  ctx.font = '12px "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(pos.name, cx, cy + baseR + 16)

  if (isAlarm) {
    ctx.fillStyle = '#f53f3f'
    ctx.font = 'bold 11px "Microsoft YaHei", sans-serif'
    ctx.fillText('积水危险', cx, cy + baseR + 32)
  } else if (isForecast) {
    ctx.fillStyle = '#ff7d00'
    ctx.font = 'bold 11px "Microsoft YaHei", sans-serif'
    ctx.fillText('前瞻预警', cx, cy + baseR + 32)
  }
}

function animate() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  animTime = performance.now()
  draw(ctx, canvas.width, canvas.height, animTime)
  animFrame = requestAnimationFrame(animate)
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const parent = canvas.parentElement
  canvas.width = parent.clientWidth
  canvas.height = parent.clientHeight
}

function handleClick(e) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const w = canvas.width
  const h = canvas.height

  for (const pos of UNDERPASS_POSITIONS) {
    const cx = pos.x * w
    const cy = pos.y * h
    const dist = Math.sqrt((mx - cx) ** 2 + (my - cy) ** 2)
    if (dist <= 35) {
      emit('select', pos.id)
      return
    }
  }
}

onMounted(() => {
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  canvasRef.value.addEventListener('click', handleClick)
  animate()
})

onUnmounted(() => {
  if (animFrame) cancelAnimationFrame(animFrame)
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<style scoped>
.canvas-map {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
