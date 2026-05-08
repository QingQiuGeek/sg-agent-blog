import request from '@/utils/request.js'

const BASE = '/api/v1/agent'

// 会话列表
export function getSessionList() {
    return request({ url: `${BASE}/sessions`, method: 'get' })
}

// 新建会话
export function createSession() {
    return request({ url: `${BASE}/sessions`, method: 'post' })
}

// 重命名会话
export function renameSession(id, title) {
    return request({ url: `${BASE}/sessions/${id}`, method: 'put', data: { title } })
}

// 删除会话
export function deleteSession(id) {
    return request({ url: `${BASE}/sessions/${id}`, method: 'delete' })
}

// 获取会话消息
export function getSessionMessages(id) {
    return request({ url: `${BASE}/sessions/${id}/messages`, method: 'get' })
}

// 发送消息（AI 回复同步返回，单次最长可能 30s+，单独放宽 timeout）
export function sendChat(payload) {
    return request({
        url: `${BASE}/chat`,
        method: 'post',
        data: payload,
        timeout: 60000,
    })
}
