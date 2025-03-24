<template>
  <div class="header">
    <div class="logo">
      <i class="el-icon-video-camera"></i>
      <span class="title">媒体流检测工具</span>
    </div>
    <div class="header-right">
      <div class="network-info" v-if="localIp">
        <div class="info-tags">
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              <div>本机IP：{{ localIp }}</div>
              <div v-if="metroIp">公网IP：{{ metroIp }}</div>
              <div>点击复制本机IP</div>
            </div>
            <el-tag 
              type="info" 
              @click="copyIp"
              style="cursor: pointer">
              {{ localIp }}
            </el-tag>
          </el-tooltip>
          <el-tooltip effect="dark" placement="bottom" :content="isp">
            <el-tag 
              type="warning"
              style="margin-left: 8px">
              {{ isp }}
            </el-tag>
          </el-tooltip>
        </div>
      </div>
      <el-tooltip :content="ipv6Loading ? '正在检测IPV6支持状态...' : '点击重新检测IPV6支持状态'" placement="bottom">
        <div class="ipv6-status" 
          :class="{ 'is-supported': ipv6Support, 'is-loading': ipv6Loading }"
          @click="getNetworkInfo">
          <i v-if="!ipv6Loading" :class="ipv6Support ? 'el-icon-check' : 'el-icon-close'"></i>
          <i v-else class="el-icon-loading"></i>
          IPV6{{ ipv6Loading ? '检测中' : (ipv6Support ? '支持' : '不支持') }}
        </div>
      </el-tooltip>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PageHeader',
  data() {
    return {
      localIp: '',
      metroIp: '',
      isp: '',
      ipv6Support: false,
      ipv6Loading: false
    }
  },
  mounted() {
    this.getNetworkInfo()
  },
  methods: {
    async getNetworkInfo() {
      if (this.ipv6Loading) return // 如果正在检测中，则不重复检测
      
      this.ipv6Loading = true
      try {
        const response = await fetch(`${this.getBaseUrl()}/api/network-info`)
        if (response.ok) {
          const data = await response.json()
          if (data.success) {
            this.localIp = data.localIp
            this.metroIp = data.metroIp
            this.isp = data.isp
            this.ipv6Support = Boolean(data.ipv6Support)
            console.log('IPv6 support status:', data.ipv6Support)
          }
        }
      } catch (error) {
        console.error('获取网络信息失败:', error)
        this.$message.error('检测失败，请重试')
      } finally {
        this.ipv6Loading = false
      }
    },
    copyIp() {
      if (this.localIp) {
        const textarea = document.createElement('textarea')
        textarea.value = this.localIp
        document.body.appendChild(textarea)
        textarea.select()
        
        try {
          document.execCommand('copy')
          this.$message.success('IP地址已复制')
        } catch (err) {
          this.$message.error('复制失败')
        } finally {
          document.body.removeChild(textarea)
        }
      }
    },
    getBaseUrl() {
      const serverIp = localStorage.getItem('serverIp')
      if (serverIp) {
        return `http://${serverIp}:8080`
      }
      return 'http://localhost:8080'
    }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 52px;
  background-color: #001529;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.logo {
  display: flex;
  align-items: center;
}

.logo i {
  font-size: 24px;
  color: #fff;
}

.title {
  margin-left: 14px;
  font-size: 18px;
  font-weight: 500;
  color: white;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.network-info .info-tags {
  display: flex;
  align-items: center;
}

.network-info .el-tag {
  height: 28px;
  line-height: 28px;
  padding: 0 12px;
  border-radius: 2px;
  font-size: 13px;
}

.network-info .el-tag:first-child {
  background-color: #1890ff;
  border-color: #1890ff;
  color: white;
}

.network-info .el-tag:first-child:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
}

.ipv6-status {
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  height: 28px;
  line-height: 28px;
  background: #f56c6c;
  color: #fff;
  border-radius: 2px;
  font-size: 13px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ipv6-status.is-supported {
  background: #67c23a;
}

.ipv6-status.is-loading {
  background: #909399;
}

.ipv6-status i {
  margin-right: 4px;
  font-size: 13px;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.el-icon-loading {
  animation: rotating 2s linear infinite;
}
</style> 