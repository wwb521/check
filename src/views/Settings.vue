<template>
  <div class="settings">
    <el-card class="settings-card">
      <div slot="header">
        <span>系统设置</span>
      </div>
      
      <el-form :model="settings" ref="settingsForm" label-width="180px">
        <el-form-item 
          label="FFprobe路径" 
          prop="ffprobePath"
          :rules="[
            { required: true, message: '请输入FFprobe路径', trigger: 'blur' }
          ]">
          <el-input 
            v-model="settings.ffprobePath" 
            placeholder="请输入FFprobe可执行文件的完整路径">
            <template slot="append">
              <el-button @click="scanFfprobe">自动扫描</el-button>
              <el-button @click="testFfprobe">测试</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="自动清除多播源">
          <el-switch
            v-model="settings.autoRemoveMulticast"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">自动清除检测到的多播源（IPTV内网组播地址）</span>
        </el-form-item>

        <el-form-item label="自动清除异常源">
          <el-switch
            v-model="settings.autoRemoveInvalid"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">开启后将自动清除检测失败的直播源</span>
        </el-form-item>

        <el-form-item label="自动清除低分辨率">
          <el-switch
            v-model="settings.autoRemoveLowRes"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">开启后将自动清除分辨率低于{{ settings.extremeMode ? '1920x1080' : (settings.strictMode ? '1920x1080' : '1280x720') }}的直播源</span>
        </el-form-item>

        <el-form-item label="自动清除慢速源">
          <el-switch
            v-model="settings.autoRemoveSlowResponse"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">开启后将自动清除响应时间超过{{ settings.extremeMode ? '1500' : (settings.strictMode ? '2000' : '3000') }}ms的直播源</span>
        </el-form-item>

        <el-form-item label="严格模式">
          <el-switch
            v-model="settings.strictMode"
            :disabled="settings.extremeMode"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">
            {{ settings.extremeMode ? '请先关闭极端模式' : '开启后将使用更严格的清除标准（1920x1080和2000ms）' }}
          </span>
        </el-form-item>

        <el-form-item label="极端模式">
          <el-switch
            v-model="settings.extremeMode"
            :disabled="settings.strictMode"
            @change="handleSettingChange">
          </el-switch>
          <span class="setting-tip">
            {{ settings.strictMode ? '请先关闭严格模式' : '开启后将使用最严格的清除标准（1920x1080和1500ms）' }}
          </span>
        </el-form-item>

        <el-form-item 
          label="服务器IP地址" 
          prop="serverIp"
          :rules="[
            { required: true, message: '请输入服务器IP地址', trigger: 'blur' },
            { pattern: /^(\d{1,3}\.){3}\d{1,3}$/, message: '请输入正确的IP地址格式', trigger: 'blur' }
          ]">
          <el-input 
            v-model="settings.serverIp" 
            placeholder="请输入服务器IP地址"
            @change="handleSettingChange">
            <template slot="append">
              <el-button @click="testServerIp">测试连接</el-button>
            </template>
          </el-input>
          <span class="setting-tip">填写后，所有接口请求将使用此IP地址</span>
        </el-form-item>

        <el-form-item 
          label="并发检测数量" 
          prop="concurrentChecks"
          :rules="[
            { required: true, message: '请设置并发检测数量', trigger: 'change' },
            { type: 'number', min: 1, max: 100, message: '并发数必须在1到100之间', trigger: 'change' }
          ]">
          <el-input-number 
            v-model="settings.concurrentChecks"
            :min="1"
            :max="100"
            :step="1"
            @change="handleSettingChange">
          </el-input-number>
          <span class="setting-tip">同时检测的直播源数量 (1-100个)</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'SettingsPage',
  data() {
    return {
      settings: {
        ffprobePath: localStorage.getItem('ffprobePath') || '',
        autoRemoveMulticast: localStorage.getItem('autoRemoveMulticast') === 'true',
        autoRemoveInvalid: localStorage.getItem('autoRemoveInvalid') === 'true',
        autoRemoveLowRes: localStorage.getItem('autoRemoveLowRes') === 'true',
        autoRemoveSlowResponse: localStorage.getItem('autoRemoveSlowResponse') === 'true',
        concurrentChecks: parseInt(localStorage.getItem('concurrentChecks')) || 3,
        strictMode: localStorage.getItem('strictMode') === 'true',
        extremeMode: localStorage.getItem('extremeMode') === 'true',
        serverIp: localStorage.getItem('serverIp') || ''
      }
    }
  },
  watch: {
    // 监听设置变化，自动保存
    'settings.ffprobePath'() {
      this.handleSettingChange()
    },
    'settings.autoRemoveMulticast'() {
      this.handleSettingChange()
    },
    'settings.autoRemoveInvalid'() {
      this.handleSettingChange()
    },
    'settings.autoRemoveLowRes'() {
      this.handleSettingChange()
    },
    'settings.autoRemoveSlowResponse'() {
      this.handleSettingChange()
    },
    'settings.concurrentChecks'() {
      this.handleSettingChange()
    },
    'settings.strictMode'() {
      this.handleSettingChange()
    },
    'settings.extremeMode'() {
      this.handleSettingChange()
    },
    'settings.serverIp'(newVal) {
      this.handleSettingChange()
      // 检查是否需要更新地址栏
      const oldServerIp = localStorage.getItem('serverIp')
      const serverIp = localStorage.getItem('serverIp')
      
      // 只有当服务器IP设置发生变化时才更新地址栏
      if (oldServerIp !== newVal) {
        const currentPort = window.location.port
        const newHost = newVal && serverIp ? serverIp : 'localhost'
        const protocol = window.location.protocol
        const pathname = window.location.pathname
        const search = window.location.search
        
        const newUrl = `${protocol}//${newHost}:${currentPort}${pathname}${search}`
        
        if (window.location.href !== newUrl) {
          this.$message.success('设置已更新，即将刷新页面...')
          setTimeout(() => {
            window.location.href = newUrl
          }, 1000)
        }
      }
    }
  },
  methods: {
    async testFfprobe() {
      if (!this.settings.ffprobePath) {
        this.$message.warning('请先输入FFprobe路径')
        return
      }

      this.$message.info('正在测试FFprobe...')
      try {
        const response = await fetch(`${this.getBaseUrl()}/api/test-ffprobe`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ path: this.settings.ffprobePath })
        })
        
        if (!response.ok) throw new Error('请求失败')
        
        const result = await response.json()
        if (result.success) {
          this.$message.success('FFprobe测试成功')
        } else {
          throw new Error(result.message)
        }
      } catch (error) {
        this.$message.error('FFprobe测试失败: ' + error.message)
      }
    },
    
    handleSettingChange() {
      // 保存所有设置到localStorage
      const settings = {
        ffprobePath: this.settings.ffprobePath,
        autoRemoveMulticast: this.settings.autoRemoveMulticast,
        autoRemoveInvalid: this.settings.autoRemoveInvalid,
        autoRemoveLowRes: this.settings.autoRemoveLowRes,
        autoRemoveSlowResponse: this.settings.autoRemoveSlowResponse,
        concurrentChecks: this.settings.concurrentChecks,
        strictMode: this.settings.strictMode,
        extremeMode: this.settings.extremeMode,
        serverIp: this.settings.serverIp
      }
      
      // 分别保存每个设置项
      Object.entries(settings).forEach(([key, value]) => {
        localStorage.setItem(key, typeof value === 'boolean' ? value.toString() : value)
      })
      
      // 同时保存到 appSettings
      localStorage.setItem('appSettings', JSON.stringify(settings))
      
      // 触发设置更新事件
      this.$root.$emit('settings-updated', {
        type: 'settings-changed',
        settings: settings
      })

      console.log('设置已保存:', settings)
    },

    getBaseUrl() {
      const serverIp = localStorage.getItem('serverIp')
      if (serverIp) {
        return `http://${serverIp}:8080`
      }
      return 'http://localhost:8080'
    },

    async scanFfprobe() {
      this.$message.info('正在扫描FFprobe...')
      try {
        const response = await fetch(`${this.getBaseUrl()}/api/scan-ffprobe`)
        if (!response.ok) throw new Error('请求失败')
        
        const result = await response.json()
        if (result.success) {
          this.settings.ffprobePath = result.message
          this.handleSettingChange()
          this.$message.success('已找到FFprobe')
        } else {
          this.$message.warning('未找到FFprobe，请手动输入路径')
        }
      } catch (error) {
        this.$message.error('扫描失败: ' + error.message)
      }
    },

    async testServerIp() {
      if (!this.settings.serverIp) {
        this.$message.warning('请先输入服务器IP地址')
        return
      }

      this.$message.info('正在测试服务器连接...')
      try {
        const response = await fetch(`${this.getBaseUrl()}/api/test-server`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ ip: this.settings.serverIp })
        })
        
        if (!response.ok) throw new Error('请求失败')
        
        const result = await response.json()
        if (result.success) {
          this.$message.success('服务器连接测试成功')
        } else {
          throw new Error(result.message)
        }
      } catch (error) {
        this.$message.error('服务器连接测试失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.settings {
  padding: 40px 20px 20px 20px;
}

.settings-card {
  max-width: 1000px;
  margin: 0 auto;
  margin-top: -50px;
}

.setting-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}
</style> 