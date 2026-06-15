import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export function fetchAllStatus() {
  return http.get('/underpass/status').then(r => r.data)
}

export function fetchUnderpassStatus(id) {
  return http.get(`/underpass/${id}/status`).then(r => r.data)
}

export function fetchHistory(id) {
  return http.get(`/underpass/${id}/history`).then(r => r.data)
}

export function sendHydraulicCommand(underpassId, action, heightCm) {
  return http.post('/hydraulic/command', { underpassId, action, heightCm }).then(r => r.data)
}

export function controlLed(underpassId, ledId, mode, displayText, color) {
  return http.post('/led/control', { underpassId, ledId, mode, displayText, color }).then(r => r.data)
}

export function linkUpstreamCatchment(underpassId, upstreamCatchmentId) {
  return http.post(`/underpass/${underpassId}/upstream`, { upstreamCatchmentId }).then(r => r.data)
}

export function fetchUpstreamList() {
  return http.get('/upstream/list').then(r => r.data)
}

export function fetchUpstream(id) {
  return http.get(`/upstream/${id}`).then(r => r.data)
}

export function createUpstream(data) {
  return http.post('/upstream', data).then(r => r.data)
}

export function updateUpstream(id, data) {
  return http.put(`/upstream/${id}`, data).then(r => r.data)
}

export function deleteUpstream(id) {
  return http.delete(`/upstream/${id}`).then(r => r.data)
}
