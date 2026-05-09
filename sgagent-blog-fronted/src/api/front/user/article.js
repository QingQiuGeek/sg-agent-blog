import request from "@/utils/request.js";

// 基础路径：前台「我的文章」
const BASE_URL = '/api/v1/user/articles'

// 1. 获取详情（用于编辑回显）
export function getMyArticleById(id) {
    return request({
        url: `${BASE_URL}/${id}`,
        method: 'get'
    })
}

// 2. 分页查询我的文章
export function getMyArticlePage(query) {
    return request({
        url: BASE_URL,
        method: 'get',
        params: query
    })
}

// 3. 新增文章
export function addMyArticle(data) {
    return request({
        url: BASE_URL,
        method: 'post',
        data: data
    })
}

// 4. 修改文章
export function updateMyArticle(data) {
    return request({
        url: BASE_URL,
        method: 'put',
        data: data
    })
}

// 5. 删除单个
export function deleteMyArticle(id) {
    return request({
        url: `${BASE_URL}/${id}`,
        method: 'delete'
    })
}

// 6. 批量删除
export function deleteMyArticles(ids) {
    return request({
        url: `${BASE_URL}/batch`,
        method: 'delete',
        data: ids
    })
}

// 7. AI 生成摘要
export function generateMyArticleSummary(data) {
    return request({
        url: `${BASE_URL}/summary-generation`,
        method: 'post',
        data: data,
        timeout: 60000
    })
}
