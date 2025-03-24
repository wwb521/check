<template>
  <el-menu
    :default-active="$route.path"
    class="sidebar-menu"
    background-color="#001529"
    text-color="rgba(255, 255, 255, 0.65)"
    active-text-color="#1890ff"
    :collapse="isCollapse"
    :collapse-transition="false"
    router>
    
    <el-menu-item index="/">
      <i class="el-icon-video-play"></i>
      <span slot="title">直播源检测</span>
    </el-menu-item>
    
    <el-menu-item index="/settings">
      <i class="el-icon-setting"></i>
      <span slot="title">系统设置</span>
    </el-menu-item>
  </el-menu>
</template>

<script>
export default {
  name: 'PageSidebar',
  props: {
    isCollapse: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  border-right: none;
  width: auto !important;
}

.el-menu-item {
  font-size: 14px;
  margin: 4px 0;
  height: 40px;
  line-height: 40px;
}

.el-menu-item:hover {
  background-color: #1890ff !important;
  color: white !important;
}

.el-menu-item.is-active {
  background-color: #1890ff !important;
  color: white !important;
}

.el-menu-item i {
  color: inherit;
  font-size: 18px;
  margin-right: 10px;
}

/* 折叠时的样式 */
.el-menu--collapse {
  width: 64px !important;
}

.el-menu--collapse .el-menu-item i {
  margin-right: 0;
}
</style> 