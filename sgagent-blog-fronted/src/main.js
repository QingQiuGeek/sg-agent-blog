import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import './assets/css/global.css'
import './assets/css/common.css'
import zhCn from "element-plus/es/locale/lang/zh-cn";

import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { config as mdEditorConfig } from 'md-editor-v3'

// AI 图片工具会返回 raw <img> 标签，需要打开 markdown-it 的 html 选项才能渲染
mdEditorConfig({
    markdownItConfig(md) {
        md.set({ html: true })
    },
})

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.config.compilerOptions = {
    isCustomElement: (tag) => tag === 'emoji-picker'
}

app.use(createPinia())

app.use(router)
app.use(ElementPlus,{
    locale:zhCn,
})

app.mount('#app')

