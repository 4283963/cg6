import { ref, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const latestUpdates = ref({})

let stompClient = null

export function useWebSocket() {
  function connect() {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws/underpass'),
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient.subscribe('/topic/underpass/update', (message) => {
          try {
            const data = JSON.parse(message.body)
            latestUpdates.value[data.underpassId] = data
          } catch (e) {
            console.error('WS parse error:', e)
          }
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      }
    })
    stompClient.activate()
  }

  function disconnect() {
    if (stompClient) {
      stompClient.deactivate()
    }
  }

  onMounted(() => connect())
  onUnmounted(() => disconnect())

  return { latestUpdates }
}
