import request from "@/utils/request.js";

// 基础路径：前台「我的知识库」
const BASE_URL = '/api/v1/user/kbs'

// 1. 我的知识库列表
export function getMyKbs() {
    return request({
        url: BASE_URL,
        method: 'get'
    })
}

// 2. 知识库简表（用于 SgAgent 输入框多选）
export function getMyKbsBrief() {
    return request({
        url: `${BASE_URL}/brief`,
        method: 'get'
    })
}

// 3. 知识库详情
export function getKb(kbId) {
    return request({
        url: `${BASE_URL}/${kbId}`,
        method: 'get'
    })
}

// 4. 新建知识库
export function addKb(data) {
    return request({
        url: BASE_URL,
        method: 'post',
        data
    })
}

// 5. 修改知识库
export function updateKb(data) {
    return request({
        url: BASE_URL,
        method: 'put',
        data
    })
}

// 6. 删除知识库
export function deleteKb(kbId) {
    return request({
        url: `${BASE_URL}/${kbId}`,
        method: 'delete'
    })
}

// 7. 知识库文件列表
export function getKbFiles(kbId) {
    return request({
        url: `${BASE_URL}/${kbId}/files`,
        method: 'get'
    })
}

// 8. 上传文件到知识库
export function uploadKbFile(kbId, formData) {
    return request({
        url: `${BASE_URL}/${kbId}/files`,
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000 // 大文件 + Tika 提取 + 异步触发，放宽到 2 分钟
    })
}

// 9. 删除知识库文件
export function deleteKbFile(fileId) {
    return request({
        url: `${BASE_URL}/files/${fileId}`,
        method: 'delete'
    })
}
