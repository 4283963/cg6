<template>
  <div class="upstream-manager">
    <div class="panel-title">
      <span>上游汇水区管理</span>
      <button class="btn-add" @click="showCreateForm = true">+ 新增</button>
    </div>

    <div class="upstream-list">
      <div
        v-for="c in catchments"
        :key="c.id"
        class="upstream-item"
        @click="selectCatchment(c)"
        :class="{ selected: selectedId === c.id }"
      >
        <div class="item-head">
          <span class="c-name">{{ c.name }}</span>
          <span class="c-id">{{ c.id }}</span>
        </div>
        <div class="item-meta">
          <span>流量计: {{ c.flowMeterId }}</span>
          <span>雨量: {{ c.rainSensorId || '-' }}</span>
        </div>
        <div v-if="c.description" class="item-desc">{{ c.description }}</div>
        <div class="item-actions" @click.stop>
          <button class="btn-edit" @click="editCatchment(c)">编辑</button>
          <button class="btn-del" @click="removeCatchment(c)">删除</button>
        </div>
      </div>
      <div v-if="catchments.length === 0" class="empty">暂无汇水区，请新增</div>
    </div>

    <div v-if="showCreateForm || editing" class="form-modal">
      <div class="form-box">
        <div class="form-title">{{ showCreateForm ? '新增汇水区' : '编辑汇水区' }}</div>

        <div class="form-row">
          <label>编号</label>
          <input v-model="form.id" :disabled="editing" placeholder="如 CATCH-006" />
        </div>
        <div class="form-row">
          <label>名称</label>
          <input v-model="form.name" placeholder="如 新华路上游排水渠" />
        </div>
        <div class="form-row">
          <label>流量计编号</label>
          <input v-model="form.flowMeterId" placeholder="如 FM-006" />
        </div>
        <div class="form-row">
          <label>雨量传感器编号</label>
          <input v-model="form.rainSensorId" placeholder="如 RAIN-006 (可选)" />
        </div>
        <div class="form-row">
          <label>经度</label>
          <input v-model.number="form.longitude" type="number" step="0.0001" />
        </div>
        <div class="form-row">
          <label>纬度</label>
          <input v-model.number="form.latitude" type="number" step="0.0001" />
        </div>
        <div class="form-row">
          <label>描述</label>
          <input v-model="form.description" placeholder="可选" />
        </div>

        <div class="form-actions">
          <button class="btn-cancel" @click="closeForm">取消</button>
          <button class="btn-save" @click="saveForm">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { fetchUpstreamList, createUpstream, updateUpstream, deleteUpstream } from '../api/underpass.js'

const emit = defineEmits(['select'])

const catchments = ref([])
const selectedId = ref(null)
const showCreateForm = ref(false)
const editing = ref(false)
const form = reactive({
  id: '', name: '', flowMeterId: '', rainSensorId: '',
  longitude: 116.40, latitude: 39.90, description: ''
})

async function loadList() {
  catchments.value = await fetchUpstreamList()
}

function resetForm() {
  Object.assign(form, {
    id: '', name: '', flowMeterId: '', rainSensorId: '',
    longitude: 116.40, latitude: 39.90, description: ''
  })
}

function selectCatchment(c) {
  selectedId.value = c.id
  emit('select', c)
}

function editCatchment(c) {
  editing.value = true
  showCreateForm.value = false
  Object.assign(form, c)
}

function closeForm() {
  showCreateForm.value = false
  editing.value = false
  resetForm()
}

async function saveForm() {
  if (!form.id || !form.name || !form.flowMeterId) {
    alert('请填写编号、名称、流量计编号')
    return
  }
  try {
    if (showCreateForm.value) {
      await createUpstream({ ...form })
    } else {
      await updateUpstream(form.id, { ...form })
    }
    closeForm()
    await loadList()
  } catch (e) {
    alert('保存失败：' + e.message)
  }
}

async function removeCatchment(c) {
  if (!confirm(`确认删除汇水区 "${c.name}" ?`)) return
  await deleteUpstream(c.id)
  if (selectedId.value === c.id) selectedId.value = null
  await loadList()
}

onMounted(loadList)
defineExpose({ loadList, selectedId })
</script>

<style scoped>
.upstream-manager {
  padding: 16px;
  border-top: 1px solid rgba(64, 158, 255, 0.15);
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
}

.btn-add, .btn-edit, .btn-del, .btn-cancel, .btn-save {
  border: none;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.btn-add {
  background: rgba(64, 158, 255, 0.2);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.4);
}

.btn-edit {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
}

.btn-del {
  background: rgba(245, 63, 63, 0.15);
  color: #f53f3f;
}

.btn-cancel {
  background: rgba(134, 144, 156, 0.2);
  color: #86909c;
}

.btn-save {
  background: #409eff;
  color: #fff;
}

.upstream-list {
  max-height: 300px;
  overflow-y: auto;
}

.upstream-item {
  padding: 8px 10px;
  margin-bottom: 6px;
  background: #0f1f35;
  border: 1px solid rgba(64, 158, 255, 0.1);
  border-radius: 4px;
  cursor: pointer;
}

.upstream-item.selected {
  border-color: #409eff;
}

.item-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.c-name { font-size: 13px; color: #e0e8f0; font-weight: 500; }
.c-id { font-size: 11px; color: #86909c; }

.item-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #86909c;
  margin-bottom: 4px;
}

.item-desc {
  font-size: 11px;
  color: #5a6a7a;
  margin-bottom: 6px;
}

.item-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.empty {
  text-align: center;
  color: #5a6a7a;
  font-size: 12px;
  padding: 20px 0;
}

.form-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.form-box {
  background: #0d1e36;
  border: 1px solid rgba(64, 158, 255, 0.3);
  border-radius: 8px;
  padding: 20px;
  width: 360px;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
}

.form-row label {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 4px;
}

.form-row input {
  background: #0a1628;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 4px;
  padding: 6px 10px;
  color: #e0e8f0;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
