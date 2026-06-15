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
        <span class="depth-bar">
          <span
            class="depth-fill"
            :style="{ width: Math.min(100, (underpass.currentDepthMm || 0) / 2) + '%' }"
            :class="{ danger: (underpass.currentDepthMm || 0) >= 100 }"
          ></span>
        </span>
      </div>

      <div class="info-label">状态</div>
      <div class="info-value">
        <span class="status-badge" :class="underpass.status === 'ALARM' ? 'badge-alarm' : 'badge-normal'">
          {{ underpass.status === 'ALARM' ? '告警' : '正常' }}
        </span>
      </div>

      <div class="info-label">液压井盖</div>
      <div class="info-value">
        {{ underpass.hydraulicLifted ? '已顶升 15cm' : '已降下' }}
      </div>

      <div class="info-label">LED警示灯</div>
      <div class="info-value">
        {{ underpass.ledAlarmActive ? '红色告警' : '绿色正常' }}
      </div>

      <div class="info-label">坐标</div>
      <div class="info-value small">
        {{ (underpass.longitude || 0).toFixed(4) }}, {{ (underpass.latitude || 0).toFixed(4) }}
      </div>
    </div>

    <div class="panel-actions">
      <button class="btn btn-lift" @click="$emit('lift', underpass.underpassId)">
        ⬆ 手动顶升
      </button>
      <button class="btn btn-lower" @click="$emit('lower', underpass.underpassId)">
        ⬇ 手动降下
      </button>
    </div>
  </div>

  <div class="detail-panel empty" v-else>
    <div class="panel-title">监测详情</div>
    <div class="empty-text">请选择一个地道桥监测点</div>
  </div>
</template>

<script setup>
defineProps({
  underpass: { type: Object, default: null }
})
defineEmits(['lift', 'lower'])
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

.info-value.small {
  font-size: 11px;
  color: #86909c;
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

@keyframes badge-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.panel-actions {
  display: flex;
  gap: 10px;
}

.btn {
  flex: 1;
  padding: 8px 0;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
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
