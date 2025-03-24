<template>
  <el-container class="app-wrapper">
    <el-header height="60px">
      <page-header />
    </el-header>
    
    <el-container>
      <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar-container">
        <div class="collapse-btn" @click="toggleSidebar">
          <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
        </div>
        <page-sidebar @select="handleMenuSelect" :is-collapse="isCollapse" />
      </el-aside>
      
      <el-main>
        <transition name="fade-transform" mode="out-in">
          <keep-alive>
            <router-view ref="currentView"></router-view>
          </keep-alive>
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import PageHeader from './components/Header.vue'
import PageSidebar from './components/Sidebar.vue'

export default {
  name: 'App',
  components: {
    PageHeader,
    PageSidebar
  },
  data() {
    return {
      isCollapse: localStorage.getItem('sidebarCollapsed') === 'true'
    }
  },
  methods: {
    handleMenuSelect() {
      // 不需要特殊处理，让 keep-alive 来维护组件状态
    },
    toggleSidebar() {
      this.isCollapse = !this.isCollapse
      localStorage.setItem('sidebarCollapsed', this.isCollapse)
    }
  }
}
</script>

<style>
/* 页面切换动画 */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.5s;
}

.fade-transform-enter {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.app-wrapper {
  height: 100%;
}

.el-header {
  background-color: #001529;
  color: rgba(255, 255, 255, 0.65);
  line-height: 60px;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.el-aside {
  background-color: #001529;
  color: rgba(255, 255, 255, 0.65);
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
  height: calc(100vh - 60px);
  overflow: hidden;
  position: relative;
}

/* 修改动画容器样式 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  position: absolute;
  left: 20px;
  right: 20px;
  top: 20px;
  height: calc(100% - 40px);  /* 减去上下padding */
  overflow-y: auto;
  /* 添加滚动条样式 */
  scrollbar-width: thin;  /* Firefox */
  scrollbar-color: rgba(0,0,0,0.3) transparent;  /* Firefox */
}

/* Webkit浏览器的滚动条样式 */
.fade-transform-enter-active::-webkit-scrollbar,
.fade-transform-leave-active::-webkit-scrollbar {
  width: 6px;
}

.fade-transform-enter-active::-webkit-scrollbar-thumb,
.fade-transform-leave-active::-webkit-scrollbar-thumb {
  background-color: rgba(0,0,0,0.3);
  border-radius: 3px;
}

.fade-transform-enter-active::-webkit-scrollbar-track,
.fade-transform-leave-active::-webkit-scrollbar-track {
  background-color: transparent;
}

html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}

#app {
  height: 100%;
}

.sidebar-container {
  position: relative;
  transition: width 0.3s;
  background-color: #001529;
  overflow: hidden;
}

.collapse-btn {
  position: absolute;
  right: 0;
  bottom: 20px;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: #001529;
  color: #fff;
  border-radius: 4px 0 0 4px;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s;
  box-shadow: -2px 0 8px rgba(0,0,0,0.15);
  opacity: 0.8;
}

.collapse-btn:hover {
  background-color: #1890ff;
  opacity: 1;
}

.collapse-btn i {
  font-size: 16px;
  line-height: 24px;
}
</style> 