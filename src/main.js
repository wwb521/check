import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'

Vue.use(ElementUI)

// 配置全局消息提示
Vue.prototype.$message.config = {
  top: '60px',        // 距离顶部60px，避开标题栏
  duration: 1000,     // 显示1秒后自动关闭
  offset: 20          // 多个消息之间的距离
}

Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')