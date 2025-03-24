<template>
  <!-- 设置组件的模板 -->
</template>

<script>
export default {
  data() {
    return {
      ffprobePath: '',
      persistentData: false,
      autoRemoveInvalid: false,
      autoRemoveLowRes: false,
      autoRemoveSlowResponse: false,
      autoRemoveMulticast: false,
      extremeMode: false,
      strictMode: false
    }
  },
  created() {
    // 初始化默认设置
    const defaultSettings = {
      ffprobePath: '',
      persistentData: false,
      autoRemoveInvalid: false,
      autoRemoveLowRes: false,
      autoRemoveSlowResponse: false,
      autoRemoveMulticast: false,
      extremeMode: false,
      strictMode: false
    }

    // 从localStorage加载设置
    try {
      const savedSettings = localStorage.getItem('appSettings')
      if (savedSettings) {
        const settings = JSON.parse(savedSettings)
        // 确保所有设置都被正确加载
        Object.keys(defaultSettings).forEach(key => {
          // 确保布尔值被正确处理
          if (typeof defaultSettings[key] === 'boolean') {
            this[key] = settings[key] === true
          } else {
            this[key] = settings[key] !== undefined ? settings[key] : defaultSettings[key]
          }
        })
      }
      // 无论是否有保存的设置，都保存一次以确保格式正确
      this.saveSettings()
    } catch (error) {
      console.error('加载设置失败:', error)
      // 使用默认值
      Object.assign(this, defaultSettings)
      // 保存默认设置
      this.saveSettings()
    }
  },
  methods: {
    handleSwitchChange(key) {
      // 确保布尔值被正确处理
      if (typeof this[key] === 'boolean') {
        this[key] = !this[key] // 切换布尔值
      }
      
      console.log(`设置已更改 - ${key}:`, this[key])
      
      // 立即保存设置
      this.saveSettings()
      
      // 发送更新事件
      this.$root.$emit('settings-updated', {
        type: key,
        value: this[key]
      })
      
      // 显示设置更改提示
      if (key === 'autoRemoveMulticast') {
        this.$message.success(this[key] ? '已启用自动清除多播源' : '已禁用自动清除多播源')
        console.log('多播源设置已更改为:', this[key])
      }
    },
    saveSettings() {
      try {
        // 确保所有布尔值设置被正确处理
        const settings = {
          ffprobePath: this.ffprobePath || '',
          persistentData: Boolean(this.persistentData),
          autoRemoveInvalid: Boolean(this.autoRemoveInvalid),
          autoRemoveLowRes: Boolean(this.autoRemoveLowRes),
          autoRemoveSlowResponse: Boolean(this.autoRemoveSlowResponse),
          autoRemoveMulticast: Boolean(this.autoRemoveMulticast),
          extremeMode: Boolean(this.extremeMode),
          strictMode: Boolean(this.strictMode)
        }

        // 保存到localStorage
        localStorage.setItem('appSettings', JSON.stringify(settings))
        console.log('保存设置成功:', settings)
      } catch (error) {
        console.error('保存设置失败:', error)
        this.$message.error('保存设置失败')
      }
    }
  },
  mounted() {
    // 在组件挂载后立即保存一次设置，确保初始值被正确保存
    this.saveSettings()
  }
}
</script>