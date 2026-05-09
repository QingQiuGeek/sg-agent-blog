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

// 删除单条消息（用于单条删除与重试前清理历史）
export function deleteMessage(id) {
    return request({ url: `${BASE}/messages/${id}`, method: 'delete' })
}

// 上传 Agent 对话附件（OSS + Tika 提取，返回 {url, name, size, ext, content}）
export function uploadAgentFile(file) {
    const fd = new FormData()
    fd.append('file', file)
    return request({
        url: `${BASE}/files/upload`,
        method: 'post',
        data: fd,
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
    })
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

/**
 * SSE 流式发送消息
 * @param {{sessionId?: string, content: string}} payload
 * @param {{onMeta?: Function, onToolCall?: Function, onSources?: Function,
 *   onDelta?: Function, onDone?: Function, onError?: Function}} handlers
 * @returns {{abort: () => void, done: Promise<void>}}
 *
 * 使用 fetch + ReadableStream 而不是 EventSource，因为 EventSource 不支持自定义 token 头。
 */
export function streamChat(payload, handlers = {}) {
    const controller = new AbortController()
    const baseURL = import.meta.env.DEV ? 'http://localhost:8080' : ''
    const token = localStorage.getItem('token') || ''

    const done = (async () => {
        let resp
        try {
            resp = await fetch(`${baseURL}${BASE}/chat/stream`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json;charset=utf-8',
                    'Accept': 'text/event-stream',
                    token,
                },
                body: JSON.stringify(payload),
                signal: controller.signal,
            })
        } catch (e) {
            if (e.name !== 'AbortError') handlers.onError?.(e)
            return
        }

        if (!resp.ok || !resp.body) {
            handlers.onError?.(new Error(`SSE 连接失败 ${resp.status}`))
            return
        }

        const reader = resp.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        try {
            while (true) {
                const {done: streamDone, value} = await reader.read()
                if (streamDone) break
                buffer += decoder.decode(value, {stream: true})

                // SSE 以 \n\n 分隔事件
                let idx
                while ((idx = buffer.indexOf('\n\n')) !== -1) {
                    const raw = buffer.slice(0, idx)
                    buffer = buffer.slice(idx + 2)
                    dispatchEvent(raw, handlers)
                }
            }
        } catch (e) {
            if (e.name !== 'AbortError') handlers.onError?.(e)
        }
    })()

    return {
        abort: () => controller.abort(),
        done,
    }
}

/** 解析单条 SSE 事件块（形如 "event: meta\ndata: {...}"） */
function dispatchEvent(raw, handlers) {
    let event = 'message'
    const dataLines = []
    for (const line of raw.split('\n')) {
        if (!line || line.startsWith(':')) continue
        if (line.startsWith('event:')) {
            event = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim())
        }
    }
    if (!dataLines.length) return
    const dataStr = dataLines.join('\n')
    let data
    try {
        data = JSON.parse(dataStr)
    } catch {
        data = dataStr
    }
    switch (event) {
        case 'meta':      handlers.onMeta?.(data); break
        case 'tool_call': handlers.onToolCall?.(data); break
        case 'sources':   handlers.onSources?.(data); break
        case 'delta':     handlers.onDelta?.(data); break
        case 'done':      handlers.onDone?.(data); break
        case 'error':     handlers.onError?.(new Error(data?.msg || 'SSE error')); break
    }
}
