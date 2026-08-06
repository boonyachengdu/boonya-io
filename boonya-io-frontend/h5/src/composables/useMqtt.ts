import mqtt from 'mqtt'
import { ref } from 'vue'

// EMQX WebSocket 端点（docker-compose 暴露 8183:8083）。可用 VITE_MQTT_URL 覆盖
const MQTT_URL = import.meta.env.VITE_MQTT_URL || 'ws://localhost:8183/mqtt'

let client: mqtt.MqttClient | null = null
const connected = ref(false)

// filter -> handler 集合
const subscriptions = new Map<string, Set<(topic: string, payload: any) => void>>()

/** MQTT 主题过滤器匹配（支持 + 与 #） */
function topicMatches(filter: string, topic: string): boolean {
  const f = filter.split('/')
  const t = topic.split('/')
  for (let i = 0; i < f.length; i++) {
    if (f[i] === '#') return true
    if (f[i] === '+') continue
    if (f[i] !== t[i]) return false
  }
  return f.length === t.length
}

function ensureClient(): mqtt.MqttClient {
  if (client) return client
  client = mqtt.connect(MQTT_URL, {
    clientId: 'h5-' + Math.random().toString(16).slice(2, 10),
    clean: true,
    reconnectPeriod: 3000,
    connectTimeout: 5000,
  })
  client.on('connect', () => {
    connected.value = true
    // 重连后重订阅
    subscriptions.forEach((_, filter) => client?.subscribe(filter))
  })
  client.on('close', () => {
    connected.value = false
  })
  client.on('error', (e) => {
    console.error('[MQTT] error:', e.message)
  })
  client.on('message', (topic, payload) => {
    let data: any
    try {
      data = JSON.parse(payload.toString())
    } catch {
      data = payload.toString()
    }
    subscriptions.forEach((handlers, filter) => {
      if (topicMatches(filter, topic)) {
        handlers.forEach((h) => h(topic, data))
      }
    })
  })
  return client
}

/**
 * 订阅 MQTT 主题。返回取消订阅函数。
 * handler 收到 (topic, payload)，payload 已尝试 JSON.parse。
 */
export function subscribe(filter: string, handler: (topic: string, payload: any) => void) {
  const c = ensureClient()
  let set = subscriptions.get(filter)
  if (!set) {
    set = new Set()
    subscriptions.set(filter, set)
    c.subscribe(filter)
  }
  set.add(handler)
  return () => {
    const s = subscriptions.get(filter)
    if (s) {
      s.delete(handler)
      if (s.size === 0) {
        subscriptions.delete(filter)
        c.unsubscribe(filter)
      }
    }
  }
}

export function useMqtt() {
  return { connected, subscribe }
}

export default useMqtt
