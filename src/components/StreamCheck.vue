<template>
  <div class="stream-check">
    <!-- 修改消息面板位置和样式 -->
    <div class="message-panel" :class="{ 'has-messages': messages.length > 0 }">
      <transition-group name="message-list">
        <div v-for="msg in messages" :key="msg.id" class="message-item">
          <i class="el-icon-delete"></i>
          <span class="message-content">{{ msg.content }}</span>
        </div>
      </transition-group>
    </div>

    <div class="table-operations">
      <div class="button-groups">
        <div class="button-group">
      <el-button-group>
        <el-button
            type="primary"
            icon="el-icon-upload2"
            @click="$refs.fileInput.click()"
            :disabled="isCollecting">
          导入本地文件
        </el-button>
        <el-button
            type="primary"
            icon="el-icon-download"
            @click="importUrlDialogVisible = true"
            :disabled="isCollecting">
          导入网络文件
        </el-button>
      </el-button-group>
        </div>

        <div class="button-group">
      <el-button-group>
        <el-button
            type="success"
            icon="el-icon-refresh"
            @click="handleBatchCheck"
            :loading="isChecking">
          {{ checkButtonText }}
        </el-button>
        <el-button
            v-if="isChecking"
            type="warning"
            icon="el-icon-video-pause"
            @click="pauseCheck">
          暂停检测
        </el-button>
        <el-button
            v-if="isPaused"
            type="primary"
            icon="el-icon-video-play"
            @click="resumeCheck">
          继续检测
        </el-button>
        <el-button
            type="danger"
            icon="el-icon-delete"
            @click="handleClearList">
          清空列表
        </el-button>
      </el-button-group>
        </div>

        <div class="button-group">
      <el-button-group>
        <el-button
            :type="isCollecting ? 'danger' : 'primary'"
            icon="el-icon-connection"
            @click="handleCollectMode">
          {{ isCollecting ? '停止采集' : '采集模式' }}
        </el-button>
      </el-button-group>
        </div>

        <div class="button-group">
      <el-button-group>
        <el-button
            type="primary"
            icon="el-icon-link"
            @click="handleGenerateLink">
          生成链接
        </el-button>
        <el-button
            type="success"
            icon="el-icon-upload"
            @click="syncDialogVisible = true">
          同步到GitHub
        </el-button>
      </el-button-group>
        </div>

        <div class="button-group">
      <el-button-group>
        <el-button
            :type="isScheduled ? 'danger' : 'primary'"
            icon="el-icon-timer"
            @click="handleScheduleCheck">
          {{ isScheduled ? '停止定时' : '定时检测' }}
        </el-button>
      </el-button-group>
        </div>

        <div class="button-group">
      <el-button-group>
        <el-button
            :type="filteredList.length > 0 ? 'danger' : 'primary'"
            icon="el-icon-search"
            @click="filteredList.length > 0 ? resetSearch() : searchDialogVisible = true">
          {{ filteredList.length > 0 ? '重置' : '搜索' }}
        </el-button>
      </el-button-group>
        </div>
      </div>

      <!-- 隐藏的文件输入框 -->
      <input
          type="file"
          ref="fileInput"
          style="display: none"
          accept=".txt,.m3u,.m3u8"
          @change="handleFileUpload"
      >
    </div>

    <div class="el-table-wrapper">
      <el-table
          :data="paginatedData"
          border
          style="width: 100%">
        <el-table-column
            prop="name"
            label="名称"
            min-width="120"
            align="center">
        </el-table-column>

        <el-table-column
            prop="url"
            label="链接"
            min-width="200"
            align="center"
            show-overflow-tooltip>
        </el-table-column>

        <el-table-column
            prop="resolution"
            label="分辨率"
            width="120"
            align="center">
          <template slot-scope="scope">
            <el-tag
                :type="getResolutionTagType(scope.row.resolution)">
              {{ scope.row.resolution || '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
            prop="responseTime"
            label="响应速度"
            width="120"
            align="center">
          <template slot-scope="scope">
            <el-tag
                :type="getResponseTimeTagType(scope.row.responseTime)">
              {{ scope.row.responseTime === '-' ? '-' : scope.row.responseTime + 'ms' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
            prop="group"
            label="分组"
            width="120"
            align="center">
          <template slot="header">
            <div style="display: flex; align-items: center; justify-content: center;">
              <span>分组</span>
              <el-button
                  type="text"
                  icon="el-icon-setting"
                  style="padding: 0 0 0 5px;"
                  @click="groupSettingsDialogVisible = true">
              </el-button>
            </div>
          </template>
          <template slot-scope="scope">
            <el-tag
                type="info">
              {{ scope.row.group }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
            prop="status"
            label="状态"
            width="100"
            align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
            label="操作"
            width="120"
            align="center"
            fixed="right">
          <template slot-scope="scope">
            <el-tooltip content="复制链接" placement="top">
              <el-button
                  type="text"
                  icon="el-icon-document-copy"
                  @click="handleCopy(scope.row)">
              </el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button
                  type="text"
                  class="delete-btn"
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)">
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加分页组件 -->
    <div class="pagination-container">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredList.length || streamList.length">
      </el-pagination>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
        title="导入网络文件"
        :visible.sync="importUrlDialogVisible"
        width="500px"
        :close-on-click-modal="false"
        @closed="importUrlForm = { url: '', isChecking: false, isAccessible: null }">
      <el-form ref="importUrlForm" label-width="80px">
        <el-form-item label="文件链接" required>
          <el-input
            v-model="importUrlForm.url"
            placeholder="请输入直播源文件链接"
            @input="checkUrlAccessibility">
            <template slot="append">
              <i v-if="importUrlForm.isChecking" class="el-icon-loading"></i>
              <i v-else-if="importUrlForm.isAccessible === true" class="el-icon-success" style="color: #67C23A"></i>
              <i v-else-if="importUrlForm.isAccessible === false" class="el-icon-error" style="color: #F56C6C"></i>
            </template>
          </el-input>
        </el-form-item>
        <div v-if="importUrlForm.isAccessible === false" style="color: #F56C6C; font-size: 12px; margin-bottom: 10px;">
          该链接无法访问，请检查链接是否正确
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="importUrlDialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="handleImportUrl"
          :loading="importUrlForm.isChecking"
          :disabled="!importUrlForm.url || importUrlForm.isAccessible === false">
          确 定
        </el-button>
      </div>
    </el-dialog>

    <!-- 采集模式设置对话框 -->
    <el-dialog
        title="采集模式设置"
        :visible.sync="collectDialogVisible"
        width="500px">
      <el-form :model="collectForm" label-width="100px" ref="collectForm" :rules="collectRules">
        <!-- 采集模式选择 -->
        <el-form-item label="采集模式">
          <el-radio-group v-model="collectForm.mode">
            <el-radio label="all">全部采集</el-radio>
            <el-radio label="tv">央视卫视专采</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 采集地址列表 -->
        <el-form-item
            v-for="(url, index) in collectForm.urls"
            :key="index"
            :label="index === 0 ? '采集地址' : ''"
            :prop="'urls.' + index">
          <el-input
              v-model="collectForm.urls[index]"
              placeholder="请输入需要采集的网络地址"
              @input="(val) => checkCollectUrlAccessibility(val, index)">
            <template slot="append">
              <i v-if="collectForm.urlStatus && collectForm.urlStatus[index] && collectForm.urlStatus[index].isChecking" class="el-icon-loading"></i>
              <i v-else-if="collectForm.urlStatus && collectForm.urlStatus[index] && collectForm.urlStatus[index].isAccessible === true" class="el-icon-success" style="color: #67C23A"></i>
              <i v-else-if="collectForm.urlStatus && collectForm.urlStatus[index] && collectForm.urlStatus[index].isAccessible === false" class="el-icon-error" style="color: #F56C6C"></i>
              <el-button
                  icon="el-icon-delete"
                  @click="removeCollectUrl(index)"
                  v-if="collectForm.urls.length > 1">
              </el-button>
            </template>
          </el-input>
          <div v-if="collectForm.urlStatus && collectForm.urlStatus[index] && collectForm.urlStatus[index].isAccessible === false"
               style="color: #F56C6C; font-size: 12px; margin-top: 5px;">
            该链接无法访问，请检查链接是否正确
          </div>
        </el-form-item>

        <!-- 添加地址按钮和导入按钮 -->
        <el-form-item>
          <div style="display: flex; gap: 10px;">
          <el-button
              type="text"
              icon="el-icon-plus"
              @click="addCollectUrl">
            添加采集地址
          </el-button>
            <el-button
                type="text"
                icon="el-icon-upload2"
                @click="$refs.collectUrlsFileInput.click()">
              导入地址文件
            </el-button>
            <el-button
                type="text"
                icon="el-icon-delete"
                style="color: #F56C6C;"
                @click="clearCollectUrls">
              清空采集地址
            </el-button>
            <input
                type="file"
                ref="collectUrlsFileInput"
                style="display: none"
                accept=".txt"
                @change="handleCollectUrlsFileUpload">
          </div>
        </el-form-item>

        <el-form-item label="自动采集">
          <el-switch v-model="collectForm.autoCollect"></el-switch>
        </el-form-item>

        <!-- 采集时间设置 -->
        <template v-if="collectForm.autoCollect">
          <el-form-item label="采集周期" prop="collectType">
            <el-radio-group v-model="collectForm.collectType">
              <el-radio label="daily">每天</el-radio>
              <el-radio label="weekly">每周</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="执行时间" prop="executeTime">
            <el-time-picker
                v-model="collectForm.executeTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间">
            </el-time-picker>
          </el-form-item>

          <el-form-item
              label="执行日期"
              prop="weekDay"
              v-if="collectForm.collectType === 'weekly'">
            <el-select v-model="collectForm.weekDay">
              <el-option label="星期一" value="1"></el-option>
              <el-option label="星期二" value="2"></el-option>
              <el-option label="星期三" value="3"></el-option>
              <el-option label="星期四" value="4"></el-option>
              <el-option label="星期五" value="5"></el-option>
              <el-option label="星期六" value="6"></el-option>
              <el-option label="星期日" value="0"></el-option>
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer">
        <el-button @click="collectDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="startCollecting">开始采集</el-button>
      </div>
    </el-dialog>

    <!-- 生成链接对话框 -->
    <el-dialog
        title="生成链接"
        :visible.sync="generateLinkDialogVisible"
        width="500px"
        custom-class="generate-link-dialog">
      <div class="link-container">
        <!-- 添加格式选择 -->
        <el-form :model="linkForm" label-width="80px">
          <el-form-item label="文件格式">
            <el-radio-group v-model="linkForm.format">
              <el-radio label="txt">TXT格式</el-radio>
              <el-radio label="m3u">M3U格式</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <!-- 链接展示卡片 -->
        <el-card class="link-card">
          <div class="link-content">
            <el-input
                v-model="generatedLink"
                placeholder="正在生成链接..."
                readonly>
              <template slot="prepend">链接地址</template>
              <el-button
                  slot="append"
                  icon="el-icon-document-copy"
                  @click="copyGeneratedLink">
                复制
              </el-button>
            </el-input>
          </div>
        </el-card>

        <!-- 使用说明 -->
        <el-card class="tips-card">
          <div class="tips-content">
            <ul>
              <li>生成的链接可以直接在支持网络导入的播放器中使用</li>
              <li>链接内容会随列表更新而自动更新</li>
              <li>TXT格式适用于大多数播放器</li>
              <li>M3U格式提供更好的播放器兼容性</li>
            </ul>
          </div>
        </el-card>
      </div>
    </el-dialog>

    <!-- 添加定时检测设置对话框 -->
    <el-dialog
        title="定时检测设置"
        :visible.sync="scheduleDialogVisible"
        width="500px">
      <el-form :model="scheduleForm" label-width="100px" ref="scheduleForm" :rules="scheduleRules">
        <el-form-item label="检测周期" prop="scheduleType">
          <el-radio-group v-model="scheduleForm.scheduleType">
            <el-radio label="daily">每天</el-radio>
            <el-radio label="weekly">每周</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="执行时间" prop="executeTime">
          <el-time-picker
              v-model="scheduleForm.executeTime"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="选择时间">
          </el-time-picker>
        </el-form-item>

        <el-form-item
            label="执行日期"
            prop="weekDay"
            v-if="scheduleForm.scheduleType === 'weekly'">
          <el-select v-model="scheduleForm.weekDay">
            <el-option label="星期一" value="1"></el-option>
            <el-option label="星期二" value="2"></el-option>
            <el-option label="星期三" value="3"></el-option>
            <el-option label="星期四" value="4"></el-option>
            <el-option label="星期五" value="5"></el-option>
            <el-option label="星期六" value="6"></el-option>
            <el-option label="星期日" value="0"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="scheduleDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="startScheduleCheck">开始定时</el-button>
      </div>
    </el-dialog>

    <!-- 添加搜索对话框 -->
    <el-dialog
        title="搜索直播源"
        :visible.sync="searchDialogVisible"
        width="500px">
      <el-form :model="searchForm" label-width="80px">
        <el-form-item label="名称">
          <el-input
              v-model="searchForm.name"
              placeholder="支持模糊搜索"
              clearable>
          </el-input>
        </el-form-item>
        <el-form-item label="分组">
          <el-select
              v-model="searchForm.group"
              placeholder="请选择"
              clearable>
            <el-option
                v-for="group in groupOptions"
                :key="group"
                :label="group"
                :value="group">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
              v-model="searchForm.status"
              placeholder="请选择"
              clearable>
            <el-option label="正常" value="正常"></el-option>
            <el-option label="异常" value="异常"></el-option>
            <el-option label="未检测" value="未检测"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="searchDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSearch">搜 索</el-button>
      </div>
    </el-dialog>

    <!-- GitHub同步设置对话框 -->
    <el-dialog
        title="GitHub同步设置"
        :visible.sync="syncDialogVisible"
        width="500px">
      <el-form :model="syncForm" ref="syncForm" label-width="120px" :rules="syncRules">
        <el-form-item label="GitHub Token" prop="token">
          <el-input
              v-model="syncForm.token"
              type="password"
              placeholder="请输入GitHub Personal Access Token"
              show-password>
          </el-input>
        </el-form-item>
        
        <el-form-item label="仓库名称" prop="repo">
          <el-input
              v-model="syncForm.repo"
              placeholder="格式：用户名/仓库名">
          </el-input>
        </el-form-item>
        
        <el-form-item label="TXT文件名" prop="txtPath">
          <el-input
              v-model="syncForm.txtPath"
              placeholder="例如：playlist.txt">
          </el-input>
        </el-form-item>
        
        <el-form-item label="M3U文件名" prop="m3uPath">
          <el-input
              v-model="syncForm.m3uPath"
              placeholder="例如：playlist.m3u">
          </el-input>
        </el-form-item>
        
        <el-form-item label="定时同步">
          <el-switch
              v-model="syncForm.autoSync"
              @change="handleAutoSyncChange">
          </el-switch>
        </el-form-item>
        
        <template v-if="syncForm.autoSync">
          <el-form-item label="同步周期" prop="syncType">
            <el-radio-group v-model="syncForm.syncType">
              <el-radio label="daily">每天</el-radio>
              <el-radio label="weekly">每周</el-radio>
              <el-radio label="afterCheck">批量检测完毕后</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item
              label="执行时间"
              prop="executeTime"
              v-if="syncForm.syncType !== 'afterCheck'">
            <el-time-picker
                v-model="syncForm.executeTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间">
            </el-time-picker>
          </el-form-item>

          <el-form-item
              label="执行日期"
              prop="weekDay"
              v-if="syncForm.syncType === 'weekly'">
            <el-select v-model="syncForm.weekDay">
              <el-option label="星期一" value="1"></el-option>
              <el-option label="星期二" value="2"></el-option>
              <el-option label="星期三" value="3"></el-option>
              <el-option label="星期四" value="4"></el-option>
              <el-option label="星期五" value="5"></el-option>
              <el-option label="星期六" value="6"></el-option>
              <el-option label="星期日" value="0"></el-option>
            </el-select>
          </el-form-item>
        </template>
        
        <el-form-item label="提交信息" prop="message">
          <el-input
              v-model="syncForm.message"
              placeholder="更新说明">
          </el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer">
        <el-button @click="syncDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveGithubSettings">保 存</el-button>
      </div>
    </el-dialog>

    <!-- 分组设置对话框 -->
    <el-dialog
        title="分组规则设置"
        :visible.sync="groupSettingsDialogVisible"
        width="600px">
      <div class="group-settings-help">
        <p>设置格式：分组名称:关键词1#关键词2#关键词3</p>
        <p>每行一个分组规则，例如：</p>
        <p>央视频道:CCTV#cctv#CHC</p>
        <p>卫视频道:湖南#湖北#陕西</p>
      </div>
      <el-input
          type="textarea"
          v-model="groupSettingsForm.rules"
          :rows="10"
          placeholder="请输入分组规则，每行一个">
      </el-input>
      <div slot="footer">
        <el-button @click="groupSettingsDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveGroupSettings">保 存</el-button>
      </div>
    </el-dialog>
  </div>

</template>

<script>
export default {
  name: 'StreamCheck',
  data() {
    return {
      loading: false,
      streamList: [],
      importUrlDialogVisible: false,
      importUrlForm: {
        url: '',
        isChecking: false,
        isAccessible: null
      },
      importRules: {
        url: [
          { required: true, message: '请输入URL地址', trigger: 'blur' },
          { type: 'url', message: '请输入正确的URL地址', trigger: ['blur', 'change'] }
        ],
        group: [
          { required: true, message: '请选择分组', trigger: 'change' }
        ]
      },
      currentPage: 1,
      pageSize: 5,
      isChecking: false,
      isPaused: false,
      checkedCount: 0,
      totalCount: 0,
      pausedIndex: -1,
      shouldStop: false,
      isCollecting: false,
      collectDialogVisible: false,
      collectForm: {
        urls: [''],
        urlStatus: {},  // 添加urlStatus对象
        autoCollect: false,
        collectType: 'daily',
        executeTime: '09:00',
        weekDay: '1',
        mode: 'tv'
      },
      collectRules: {
        urls: [
          { 
            type: 'array',
            validator: (rule, value, callback) => {
              if (value.length === 0) {
                callback(new Error('请至少添加一个采集地址'))
              }
              // 验证每个URL
              const errors = value.map(url => {
                if (!url) return '采集地址不能为空'
                if (!/^https?:\/\/.+/.test(url)) return '请输入正确的URL地址'
                return null
              }).filter(error => error !== null)
              
              if (errors.length > 0) {
                callback(new Error(errors[0]))
              } else {
                callback()
              }
            },
            trigger: 'change'
          }
        ],
        executeTime: [
          { required: true, message: '请选择执行时间', trigger: 'change' }
        ],
        weekDay: [
          { required: true, message: '请选择执行日期', trigger: 'change' }
        ]
      },
      collectTimer: null,
      generateLinkDialogVisible: false,
      generatedLink: '',
      previewContent: '',  // 新增预览内容
      currentLinkId: '',
      scheduleDialogVisible: false,
      isScheduled: false,
      scheduleForm: {
        scheduleType: 'daily',
        executeTime: '09:00',
        weekDay: '1'
      },
      scheduleRules: {
        executeTime: [
          { required: true, message: '请选择执行时间', trigger: 'change' }
        ],
        weekDay: [
          { required: true, message: '请选择执行日期', trigger: 'change' }
        ]
      },
      scheduleTimer: null,
      nextScheduleTime: null,
      stateKey: 'streamCheckState', // 用于本地存储的key
      hasActiveOperations: false,  // 添加标记表示是否有动作在执行
      linkForm: {
        format: 'txt' // 默认txt格式
      },
      searchDialogVisible: false,
      searchForm: {
        name: '',
        group: '',
        status: ''
      },
      filteredList: [], // 用于存储搜索过滤后的列表
      realIp: '',  // 存储真实IP地址
      syncDialogVisible: false,
      syncForm: {
        token: localStorage.getItem('githubToken') || '',
        repo: localStorage.getItem('githubRepo') || '',
        txtPath: localStorage.getItem('githubTxtPath') || '',
        m3uPath: localStorage.getItem('githubM3uPath') || '',
        autoSync: localStorage.getItem('githubAutoSync') === 'true',
        syncType: localStorage.getItem('githubSyncType') || 'daily',
        executeTime: localStorage.getItem('githubExecuteTime') || '00:00',
        weekDay: localStorage.getItem('githubWeekDay') || '1',
        message: '更新直播源列表'
      },
      syncRules: {
        token: [
          { required: true, message: '请输入GitHub Token', trigger: 'blur' }
        ],
        repo: [
          { required: true, message: '请输入仓库名称', trigger: 'blur' },
          { pattern: /^[\w-]+\/[\w-]+$/, message: '格式：用户名/仓库名', trigger: 'blur' }
        ],
        txtPath: [
          { required: true, message: '请输入文件路径', trigger: 'blur' }
        ],
        m3uPath: [
          { required: true, message: '请输入文件路径', trigger: 'blur' }
        ],
        message: [
          { required: true, message: '请输入提交消息', trigger: 'blur' }
        ]
      },
      syncTimer: null,
      messages: [], // 用于存储消息
      messageId: 0, // 用于生成唯一的消息ID
      isPageSwitching: false, // 添加页面切换标志
      // 添加标准分组名称常量
      STANDARD_GROUPS: {
        CCTV: '央视频道',
        SATELLITE: '卫视频道'
      },
      groupSettingsDialogVisible: false,
      groupSettingsForm: {
        rules: ''
      },
    }
  },
  computed: {
    paginatedData() {
      // 使用过滤后的列表进行分页
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return (this.filteredList.length > 0 ? this.filteredList : this.streamList).slice(start, end)
    },
    checkButtonText() {
      if (!this.isChecking && !this.isPaused) {
        return '批量检测'
      }
      return `检测中 ${this.checkedCount}/${this.totalCount}`
    },
    mergedStreams() {
      // 创建一个Map来存储合并后的直播源
      const streamMap = new Map()
      
      // 按照当前列表顺序处理每个直播源
      this.streamList.forEach(stream => {
        const key = stream.name // 使用名称作为键
        
        if (streamMap.has(key)) {
          // 如果已存在相同名称的直播源，添加到URLs数组
          const existing = streamMap.get(key)
          if (!existing.urls.includes(stream.url)) {
            existing.urls.push(stream.url)
          }
        } else {
          // 如果是新的名称，创建新条目
          streamMap.set(key, {
            name: stream.name,
            urls: [stream.url],
            group: stream.group
          })
        }
      })
      
      // 转换回数组格式
      return Array.from(streamMap.values())
    },
    groupOptions() {
      return [...new Set(this.streamList.map(item => item.group))]
    }
  },
  created() {
    // 从localStorage加载分组设置
    const savedGroupSettings = localStorage.getItem('groupSettings')
    if (savedGroupSettings) {
      this.groupSettingsForm = JSON.parse(savedGroupSettings)
    } else {
      // 设置默认的分组规则
      this.groupSettingsForm = {
        rules: '央视频道:CCTV#cctv#CHC\n卫视频道:湖南#湖北#陕西'
      }
      // 保存默认规则到localStorage
      localStorage.setItem('groupSettings', JSON.stringify(this.groupSettingsForm))
    }

    // 初始化时加载持久化的数据
    if (localStorage.getItem('persistentData') === 'true') {
      const savedList = localStorage.getItem('streamList')
      if (savedList) {
        this.streamList = JSON.parse(savedList)
        // 初始化时应用分组规则到所有直播源
        this.streamList.forEach(stream => {
          stream.group = this.applyGroupRules(stream.name)
        })
        // 初始化时检查多播源
        this.$nextTick(() => {
          this.checkMulticastStreams()
        })
      }
    }

    // 监听保存事件
    this.$root.$on('save-stream-list', this.saveStreamList)
    
    // 监听设置更新事件
    this.$root.$on('settings-updated', this.handleSettingsUpdate)

    // 从本地存储恢复状态
    this.restoreState()
    
    // 如果定时检测是开启状态，重新启动定时器
    if (this.isScheduled) {
      this.setupSchedule()
    }
    // 如果采集是开启状态，重新启动采集
    if (this.isCollecting) {
      this.startCollecting(true) // true表示是恢复状态
    }
    // 如果GitHub自动同步是开启状态，重新启动同步定时器
    if (this.syncForm.autoSync) {
      this.setupSyncSchedule()
    }

    this.filteredList = []
    this.getRealIp()  // 获取真实IP地址
  },
  beforeDestroy() {
    // 移除事件监听
    this.$root.$off('save-stream-list', this.saveStreamList)
    this.$root.$off('settings-updated', this.handleSettingsUpdate)
    // 组件销毁前清除定时器
    if (this.collectTimer) {
      clearInterval(this.collectTimer)
    }
    // 组件销毁时清理定时器
    this.stopScheduleCheck()
    // 在组件销毁前保存状态
    this.saveState()
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
  },
  methods: {
    // 添加处理设置更新的方法
    handleSettingsUpdate(event) {
      console.log('收到设置更新事件:', event)
      if (event && event.type === 'settings-changed' && event.settings) {
        console.log('设置已更新，当前设置:', event.settings)
        // 重新检查所有多播源
        this.$nextTick(() => {
          this.checkMulticastStreams()
        })
      }
    },

    // 添加多播源检测的辅助方法
    isMulticastStream(url) {
      if (!url) return false
      
      // 更完整的多播源检测正则表达式
      const patterns = [
        // 标准格式
        /[/](udp|rtp)[/]239\.\d+\.\d+\.\d+:\d+/i,
        /[/](udp|rtp)[/]225\.\d+\.\d+\.\d+:\d+/i,
        /[/](udp|rtp)[/]224\.\d+\.\d+\.\d+:\d+/i,
        // 带@的格式
        /^(udp|rtp):\/\/@239\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/@225\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/@224\.\d+\.\d+\.\d+:\d+/i,
        // 不带@的格式
        /^(udp|rtp):\/\/239\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/225\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/224\.\d+\.\d+\.\d+:\d+/i,
        // IPv6格式
        /^(udp|rtp):\/\/\[239\.\d+\.\d+\.\d+\]:\d+/i,
        /^(udp|rtp):\/\/\[225\.\d+\.\d+\.\d+\]:\d+/i,
        /^(udp|rtp):\/\/\[224\.\d+\.\d+\.\d+\]:\d+/i,
        // 其他常见格式
        /^(udp|rtp):\/\/@?239\.\d+\.\d+\.\d+[/]\d+/i,
        /^(udp|rtp):\/\/@?225\.\d+\.\d+\.\d+[/]\d+/i,
        /^(udp|rtp):\/\/@?224\.\d+\.\d+\.\d+[/]\d+/i,
        // 简单格式
        /^udp\/@?239\.\d+\.\d+\.\d+:\d+/i,
        /^rtp\/@?239\.\d+\.\d+\.\d+:\d+/i,
        /^udp\/@?225\.\d+\.\d+\.\d+:\d+/i,
        /^rtp\/@?225\.\d+\.\d+\.\d+:\d+/i,
        /^udp\/@?224\.\d+\.\d+\.\d+:\d+/i,
        /^rtp\/@?224\.\d+\.\d+\.\d+:\d+/i,
        // 带端口的格式
        /^(udp|rtp):\/\/@?239\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/@?225\.\d+\.\d+\.\d+:\d+/i,
        /^(udp|rtp):\/\/@?224\.\d+\.\d+\.\d+:\d+/i,
        // 特殊格式
        /^(udp|rtp)[/](239|225|224)\.\d+\.\d+\.\d+[/]?(\d+)?/i,
        /^(udp|rtp):\/\/(239|225|224)\.\d+\.\d+\.\d+[/]?(\d+)?/i,
        // HTTP代理的UDP/RTP多播源格式
        /http:\/\/[^/]+[/](udp|rtp)[/]224\.\d+\.\d+\.\d+:\d+/i,
        /http:\/\/[^/]+[/](udp|rtp)[/]225\.\d+\.\d+\.\d+:\d+/i,
        /http:\/\/[^/]+[/](udp|rtp)[/]239\.\d+\.\d+\.\d+:\d+/i,
        // HTTPS代理的UDP/RTP多播源格式
        /https:\/\/[^/]+[/](udp|rtp)[/]224\.\d+\.\d+\.\d+:\d+/i,
        /https:\/\/[^/]+[/](udp|rtp)[/]225\.\d+\.\d+\.\d+:\d+/i,
        /https:\/\/[^/]+[/](udp|rtp)[/]239\.\d+\.\d+\.\d+:\d+/i,
        // 任何包含 udp 或 rtp 的链接
        /^(udp|rtp):\/\//i,
        /\/(udp|rtp)\//i,
        /^(udp|rtp)@/i
      ]
      
      // 先转换为小写进行检查
      const lowerUrl = url.toLowerCase()
      const isMulticast = patterns.some(pattern => pattern.test(lowerUrl))
            if (isMulticast) {
        console.log('检测到多播源:', url)
      }
      return isMulticast
    },

    // 检查多播源的方法
    checkMulticastStreams() {
      try {
        // 从localStorage直接获取autoRemoveMulticast设置
        const autoRemoveMulticast = localStorage.getItem('autoRemoveMulticast') === 'true'
        console.log('自动清除多播源设置状态:', autoRemoveMulticast)

        // 只有在设置为true时才执行清除
        if (autoRemoveMulticast) {
          console.log('开始检查多播源...')
          let removedCount = 0
          const toRemove = []

          // 检查所有流
          this.streamList.forEach((stream, index) => {
            if (this.isMulticastStream(stream.url)) {
              toRemove.push(index)
              removedCount++
              console.log('发现多播源:', stream.url)
            }
          })

          // 从后向前删除
          for (let i = toRemove.length - 1; i >= 0; i--) {
            const index = toRemove[i]
            const stream = this.streamList[index]
            console.log('正在删除多播源:', stream.name, stream.url)
            this.streamList.splice(index, 1)
            this.handleStreamRemoval(stream, '多播源')
          }

          if (removedCount > 0) {
            console.log(`成功清除 ${removedCount} 个多播源`)
            this.$message.warning(`已自动清除 ${removedCount} 个多播源`)
            this.saveStreamList()
            
            // 如果当前页没有数据，跳转到上一页
            if (this.paginatedData.length === 0 && this.currentPage > 1) {
              this.currentPage--
            }
          }
        } else {
          console.log('自动清除多播源功能未开启，跳过检查')
        }
      } catch (error) {
        console.error('检查多播源失败:', error)
      }
    },

    async checkStream(stream) {
      // 优先从 appSettings 中读取 FFprobe 路径
      let ffprobePath = ''
      try {
        const appSettings = localStorage.getItem('appSettings')
        if (appSettings) {
          const settings = JSON.parse(appSettings)
          ffprobePath = settings.ffprobePath
        }
      } catch (error) {
        console.error('读取 appSettings 失败:', error)
      }

      // 如果 appSettings 中没有，则从独立设置中读取
      if (!ffprobePath) {
        ffprobePath = localStorage.getItem('ffprobePath')
      }

      if (!ffprobePath) {
        console.error('未找到 FFprobe 路径配置')
        this.$message.error('请先在系统设置中配置FFprobe路径')
              return false
      }

      // 检查是否是多播源
      if (this.isMulticastStream(stream.url)) {
        // 直接返回false，不再设置状态
        return false
      }

      try {
        const response = await fetch(`${this.getBaseUrl()}/api/check-stream`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            url: stream.url,
            ffprobePath: ffprobePath
          })
        })

        if (!response.ok) throw new Error('请求失败')
        
        const result = await response.json()
        
        // 更新流信息
        stream.status = result.available ? '正常' : '异常'
        stream.resolution = result.resolution || '-'
        stream.responseTime = result.responseTime || '-'

        // 检查是否需要自动清除
        if (!result.available || this.shouldRemoveStream(stream)) {
          let reason = ''
          if (!result.available || stream.status === '异常') {
            reason = '异常源'
          } else if (stream.responseTime > 3000) {
            reason = '响应时间过慢'
          } else {
            reason = '分辨率过低'
          }

          const listIndex = this.streamList.indexOf(stream)
          if (listIndex > -1) {
            this.streamList.splice(listIndex, 1)
            this.handleStreamRemoval(stream, reason)
          }
        }
        
        this.saveStreamList()
        return result.available
      } catch (error) {
        console.error('检测错误:', error)
        stream.status = '异常'
        
        // 如果设置了自动清除异常源，在这里也进行清除
        if (this.shouldRemoveStream(stream)) {
          const currentPageItems = this.paginatedData
          const pageIndex = currentPageItems.indexOf(stream)
          const listIndex = this.streamList.indexOf(stream)
          
          if (listIndex > -1) {
            this.streamList.splice(listIndex, 1)
            if (pageIndex === currentPageItems.length - 1 && this.currentPage > 1) {
              this.currentPage--
            }
            console.log(`清除异常直播源: ${stream.name}, 原因: 检测失败`)
            this.$message.warning('已自动清除直播源：检测失败')
          }
        }
        
        this.saveStreamList()
        return false
      }
    },

    shouldRemoveStream(stream) {
      try {
        // 获取设置
        const appSettings = localStorage.getItem('appSettings')
        let settings = {}
        if (appSettings) {
          settings = JSON.parse(appSettings)
        }

        // 检查是否需要清除异常源
        if (settings.autoRemoveInvalid === true && 
            (stream.status === '异常' || stream.status === '未知')) {
          console.log(`应清除异常源: ${stream.name}, 状态: ${stream.status}`)
          return true
        }

        // 获取模式设置
        const extremeMode = settings.extremeMode === true
        const strictMode = settings.strictMode === true

        // 检查是否需要清除低分辨率源
        if (settings.autoRemoveLowRes === true && 
            stream.resolution !== '未知' && 
            stream.resolution !== '多播源' && 
            stream.resolution !== '-') {
          const [width, height] = stream.resolution.split('x').map(Number)
          if (extremeMode ? (width < 1920 || height < 1080) : 
              (strictMode ? (width < 1920 || height < 1080) : (width < 1280 || height < 720))) {
            console.log(`应清除低分辨率源: ${stream.name}, 分辨率: ${stream.resolution}`)
            return true
          }
        }

        // 检查响应时间
        if (settings.autoRemoveSlowResponse === true && 
            stream.responseTime !== '-' &&
            Number(stream.responseTime) > (extremeMode ? 1500 : (strictMode ? 2000 : 3000))) {
          console.log(`应清除慢速源: ${stream.name}, 响应时间: ${stream.responseTime}ms`)
          return true
        }

        return false
      } catch (error) {
        console.error('检查是否应该移除流时出错:', error)
        return false
      }
    },

    async handleBatchCheck() {
      if (this.isChecking) return
      
      const uncheckedStreams = this.streamList.filter(s => !this.isPaused || this.streamList.indexOf(s) > this.pausedIndex)
      if (uncheckedStreams.length === 0) {
        this.$message.info('没有需要检测的直播源')
        return
      }

      this.isChecking = true
      this.shouldStop = false
      this.totalCount = uncheckedStreams.length
      this.checkedCount = this.isPaused ? this.checkedCount : 0
      
      try {
      // 获取并发检测数量设置
      const concurrentChecks = parseInt(localStorage.getItem('concurrentChecks')) || 3
      
        // 从暂停的位置或开始位置继续检测
        const startIndex = this.isPaused ? this.pausedIndex + 1 : 0
        
        // 分批处理，每批次处理固定数量的流
        const batchSize = Math.min(concurrentChecks, 10) // 限制最大并发数
        
        for (let i = startIndex; i < uncheckedStreams.length; i += batchSize) {
          if (this.shouldStop) break
          
          // 获取当前批次要检测的流
          const batch = uncheckedStreams.slice(i, i + batchSize)
          
          // 并发检测当前批次的所有流
          await Promise.all(
            batch.map(async stream => {
              if (this.shouldStop) return
              
              try {
              stream.status = '检测中'
              const success = await this.checkStream(stream)
              this.checkedCount++
              
              if (success) {
                this.saveState()
                }
              } catch (error) {
                console.error('检测流失败:', error)
                stream.status = '异常'
                this.checkedCount++
              }
            })
          )
          
          // 更新暂停位置
          this.pausedIndex = i + batchSize - 1
          
          // 每批次完成后保存状态
          this.saveState()
        }

        if (!this.shouldStop) {
          this.isChecking = false
          this.isPaused = false
          this.pausedIndex = -1
          this.$message.success('批量检测完成')
          
          // 检查是否需要在批量检测完成后同步
          if (this.syncForm.autoSync && this.syncForm.syncType === 'afterCheck') {
            await this.syncToGithub(this.generateContent('txt'), this.syncForm.txtPath)
            await this.syncToGithub(this.generateContent('m3u'), this.syncForm.m3uPath)
            this.$message.success('已同步到GitHub')
          }
        }
      } catch (error) {
        console.error('批量检测错误:', error)
        this.$message.error('批量检测失败')
      } finally {
        if (this.shouldStop && this.streamList.length > 0) {
          this.isChecking = false
          this.isPaused = true
          this.$message.info('检测已暂停')
        } else {
          this.isChecking = false
          this.isPaused = false
          this.pausedIndex = -1
        }
      }
    },

    pauseCheck() {
      this.shouldStop = true
    },

    async resumeCheck() {
      if (this.pausedIndex >= 0) {
        this.isChecking = true
        this.isPaused = false
        this.shouldStop = false
        await this.batchCheck(this.pausedIndex)
      }
    },

    async handleCheck(row) {
      row.status = '检测中'
      
      const success = await this.checkStream(row)
      this.$message[success ? 'success' : 'error'](`检测${success ? '成功' : '失败'}: ${row.name}`)
    },
    handleDelete(row) {
      this.$confirm('确认删除该直播源?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = this.streamList.indexOf(row)
        this.streamList.splice(index, 1)
        const maxPage = Math.ceil(this.streamList.length / this.pageSize)
        if (this.currentPage > maxPage) {
          this.currentPage = Math.max(maxPage, 1)
        }
        this.saveStreamList()  // 保存更新后的列表
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    async handleUrlImport() {
      const valid = await this.$refs.importForm.validate()
      if (!valid) return

      try {
        const response = await fetch(`${this.getBaseUrl()}/api/import-url`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ url: this.importForm.url })
        })

        if (!response.ok) throw new Error('导入失败')

        const content = await response.text()
        const newStreams = this.parseContent(content)

        if (newStreams.length > 0) {
          // 过滤重复的直播源
          const existingUrls = new Set(this.streamList.map(s => s.url))
          const uniqueStreams = newStreams.filter(s => !existingUrls.has(s.url))
          
          if (uniqueStreams.length > 0) {
            // 批量添加到列表
            this.streamList.push(...uniqueStreams)
            this.$message.success(`成功导入 ${uniqueStreams.length} 个直播源`)
            this.importUrlDialogVisible = false
            
            // 导入后立即检查多播源
            this.$nextTick(() => {
              this.checkMulticastStreams()
            })
          } else {
            this.$message.info('没有新的直播源可以导入')
          }
        } else {
          this.$message.warning('未找到有效的直播源')
        }
      } catch (error) {
        this.$message.error('导入失败：' + error.message)
      }
    },
    readFile(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => resolve(e.target.result)
        reader.onerror = () => reject(new Error('文件读取失败'))
        reader.readAsText(file)
      })
    },
    parseContent(content) {
      const streams = []
      let currentGroup = '未分组'
      let currentName = ''
      
      // 获取自动清除多播源设置
      const autoRemoveMulticast = localStorage.getItem('autoRemoveMulticast') === 'true'
      console.log('解析内容时的自动清除多播源设置状态:', autoRemoveMulticast)
      
      // 清理HTML标签
      content = content.replace(/<[^>]*>/g, '')
      
      // 按行分割内容
      const lines = content.split('\n')

      lines.forEach(line => {
        line = line.trim()
        if (!line) return

        // 处理M3U格式
        if (line.startsWith('#EXTINF:-1')) {
          // 提取tvg-name属性
          const tvgNameMatch = line.match(/tvg-name="([^"]+)"/)
          if (tvgNameMatch) {
            currentName = tvgNameMatch[1].trim()
          } else {
            // 如果没有tvg-name，尝试提取最后的逗号后的名称
            const lastCommaIndex = line.lastIndexOf(',')
            if (lastCommaIndex !== -1) {
              currentName = line.substring(lastCommaIndex + 1).trim()
            }
          }
          return
        }
        
        // 处理普通分组标记
        if (line.endsWith(',#genre#')) {
          currentGroup = line.slice(0, -8)
          return
        }
        
        // 如果是URL行（不以#开头）
        if (!line.startsWith('#')) {
          let name = currentName || ''
          let urls = []
          
          // 处理普通格式（名称,URL）
          if (line.includes(',')) {
            const parts = line.split(',')
            name = name || parts[0].trim()
            // 清理URL中的HTML标签
            urls = parts.slice(1).join(',').split('#').map(url => url.replace(/<[^>]*>/g, ''))
          } else {
            // 处理纯URL格式，清理HTML标签
            urls = [line.replace(/<[^>]*>/g, '')]
          }
          
          if (name && urls.length > 0) {
            // 确定分组
            let group = currentGroup
            // 处理卫视频道
            if (/卫视/.test(name)) {
              group = this.STANDARD_GROUPS.SATELLITE
            }
            // 处理CCTV频道（不区分大小写）
            else if (/CCTV/i.test(name)) {
              group = this.STANDARD_GROUPS.CCTV
            }
            
            // 添加所有有效的URL
            urls.forEach(url => {
              url = url.trim()
              // 验证URL格式，只有在autoRemoveMulticast为false时才添加多播源
              if (url && ((!autoRemoveMulticast && this.isMulticastStream(url)) || !this.isMulticastStream(url)) && this.isValidUrl(url)) {
                streams.push({
                  name: name,
                  url: url,
                  group: group,
                  status: '未检测',
                  resolution: '-',
                  responseTime: '-'
                })
              }
            })
          }
          currentName = ''  // 重置当前名称
        }
      })

      // 最后再次检查所有流的分组
      streams.forEach(stream => {
        if (/CCTV/i.test(stream.name)) {
          stream.group = this.STANDARD_GROUPS.CCTV
        } else if (/卫视/.test(stream.name)) {
          stream.group = this.STANDARD_GROUPS.SATELLITE
        }
      })

      console.log(`解析完成: 共发现 ${streams.length} 个有效直播源`)
      return streams
    },

    // 添加URL验证方法
    isValidUrl(url) {
      try {
        // 移除HTML标签
        url = url.replace(/<[^>]*>/g, '')
        // 检查是否是有效的URL格式
        return url.startsWith('http://') || url.startsWith('https://') || url.startsWith('rtmp://') || url.startsWith('rtsp://')
      } catch (e) {
        return false
      }
    },
    addStreamsToList(streams) {
      // 标准化所有流的分组名称
      streams.forEach(stream => {
        // 应用分组规则
        stream.group = this.applyGroupRules(stream.name)
      })

      // 检查重复并添加到列表
      const existingKeys = new Set(
        this.streamList.map(s => `${s.url}|${s.group}`)
      )

      const uniqueStreams = streams.filter(stream => {
        const key = `${stream.url}|${stream.group}`
        if (existingKeys.has(key)) {
          return false
        }
        existingKeys.add(key)
        return true
      })

      this.streamList.push(...uniqueStreams)
      this.currentPage = 1
      this.saveStreamList()
    },
    handleSizeChange(val) {
      this.pageSize = val
      // 当每页条数改变时，重置当前页为第一页
      this.currentPage = 1
    },
    handleCurrentChange(val) {
      this.currentPage = val
    },
    async handleFileUpload(event) {
      const file = event.target.files[0]
      if (!file) return

      try {
        const content = await this.readFile(file)
        const newStreams = this.parseContent(content)
        
        if (newStreams.length > 0) {
          // 过滤重复的直播源（同时考虑URL和分组）
          const existingKeys = new Set(
            this.streamList.map(s => `${s.url}|${s.group}`)
          )
          const uniqueStreams = newStreams.filter(s => {
            // 先应用分组规则
            s.group = this.applyGroupRules(s.name)
            const key = `${s.url}|${s.group}`
            if (existingKeys.has(key)) {
              return false
            }
            existingKeys.add(key)
            return true
          })
          
          if (uniqueStreams.length > 0) {
            this.streamList.push(...uniqueStreams)
            this.$message.success(`成功导入 ${uniqueStreams.length} 个直播源`)
            
            // 只有在设置开启时才检查多播源
            const autoRemoveMulticast = localStorage.getItem('autoRemoveMulticast') === 'true'
            if (autoRemoveMulticast) {
              this.$nextTick(() => {
                this.checkMulticastStreams()
              })
            }
          } else {
            this.$message.info('没有新的直播源可以导入')
          }
        } else {
          this.$message.warning('未找到有效的直播源')
        }
      } catch (error) {
        this.$message.error('文件读取失败：' + error.message)
      } finally {
        // 清除文件输入，允许重复选择同一文件
        event.target.value = ''
      }
    },
    getGroupFromFileName(filename) {
      if (!filename) return '未分组'
      return this.applyGroupRules(filename)
    },
    handleClearList() {
      this.$confirm('确认清空列表吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 先停止所有检测和采集任务
        this.shouldStop = true
        this.isChecking = false
        this.isPaused = false
        this.pausedIndex = -1
        
        // 清除所有定时器
        if (this.collectTimer) {
          clearInterval(this.collectTimer)
          this.collectTimer = null
        }
        if (this.scheduleTimer) {
          clearInterval(this.scheduleTimer)
          this.scheduleTimer = null
        }
        if (this.syncTimer) {
          clearInterval(this.syncTimer)
          this.syncTimer = null
        }
        
        // 重置所有状态
        this.isCollecting = false
        this.isScheduled = false
        this.checkedCount = 0
        this.totalCount = 0
        
        // 清空列表和重置页码
        this.streamList = []
        this.currentPage = 1
        
        // 更新链接内容和保存状态
        this.updateLinkContent()
        this.saveState()
        
        this.$message({
          type: 'success',
          message: '列表已清空'
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消清空'
        })
      })
    },
    handleCopy(row) {
      // 创建一个临时输入框来实现复制功能
      const textarea = document.createElement('textarea')
      textarea.value = row.url
      document.body.appendChild(textarea)
      textarea.select()
      
      try {
        document.execCommand('copy')
        this.$message.success('链接已复制到剪贴板')
      } catch (err) {
        this.$message.error('复制失败')
      } finally {
        document.body.removeChild(textarea)
      }
    },
    saveStreamList() {
      if (localStorage.getItem('persistentData') === 'true') {
        localStorage.setItem('streamList', JSON.stringify(this.streamList))
      }
    },
    handleCollectMode() {
      if (this.isCollecting) {
        this.stopCollecting()
      } else {
        this.collectDialogVisible = true
      }
    },
    async startCollecting(isRestore = false) {
      if (!isRestore) {
        // 确保 urls 是数组
        if (!Array.isArray(this.collectForm.urls)) {
          this.$set(this.collectForm, 'urls', [''])
          return
        }
        
        // 检查所有URL是否可访问
        const hasInvalidUrls = Object.values(this.collectForm.urlStatus || {}).some(
          status => status && status.isAccessible === false
        )
        
        if (hasInvalidUrls) {
          this.$message.error('存在无法访问的链接，请检查后重试')
          return
        }

        // 检查是否有正在检测的URL
        const hasCheckingUrls = Object.values(this.collectForm.urlStatus || {}).some(
          status => status && status.isChecking
        )

        if (hasCheckingUrls) {
          this.$message.warning('有链接正在检测中，请等待检测完成')
          return
        }
        
        const valid = await this.$refs.collectForm.validate()
        if (!valid) return
      }

      this.collectDialogVisible = false
      this.isCollecting = true
      this.saveState() // 保存状态
      
      try {
        // 立即执行一次采集
        await this.collect()
        
        // 如果开启了自动采集，设置定时器
        if (this.collectForm.autoCollect) {
          this.setupCollectSchedule()
        } else {
          // 单次采集完成后停止
          this.isCollecting = false
          this.saveState()
        }
      } catch (error) {
        console.error('采集过程出错:', error)
        this.isCollecting = false
        this.saveState()
      }
    },
    stopCollecting() {
      this.isCollecting = false
      if (this.collectTimer) {
        clearInterval(this.collectTimer)
        this.collectTimer = null
      }
      this.saveState() // 保存状态
      // 只有在自动采集模式下才显示停止提示
      if (this.collectForm.autoCollect) {
        this.$message.success('已停止采集')
      }
    },
    async collect() {
      let progressMessage = null;
      try {
        if (this.isChecking) {
          this.$message.warning('检测正在进行中，暂时跳过本次采集')
          return
        }

        const total = this.collectForm.urls.length
        if (total === 0) {
          this.$message.warning('没有需要采集的地址')
          return
        }

        // 创建进度消息提示
        progressMessage = this.$message({
          message: `正在采集: 0/${total}`,
          type: 'info',
          duration: 0,
          showClose: false
        });

        let allResults = []
        let current = 0

        // 逐个处理URL，避免并行处理
        for (const url of this.collectForm.urls) {
          try {
            const response = await fetch(`${this.getBaseUrl()}/api/collect-url?url=${encodeURIComponent(url)}`, {
              method: 'GET',
              headers: {
                'Accept': 'text/plain'
              },
            })
            
            if (!response.ok) {
              throw new Error(`HTTP ${response.status}: ${response.statusText}`)
            }
            
            const content = await response.text()
            const streams = this.parseContent(content)
            
            if (streams.length > 0) {
              // 根据采集模式过滤直播源
              let filteredStreams = streams
              if (this.collectForm.mode === 'tv') {
                filteredStreams = streams.filter(s => {
                  // 处理央视频道
                  const isCCTV = /CCTV/i.test(s.name)
                  if (isCCTV) {
                    s.group = this.STANDARD_GROUPS.CCTV
                    const cctvNumber = this.extractCCTVNumber(s.name)
                    if (cctvNumber) {
                      s.name = cctvNumber
                    }
                    return true
                  }
                  // 处理卫视频道
                  const isSatellite = /卫视/.test(s.name)
                  if (isSatellite) {
                    s.group = this.STANDARD_GROUPS.SATELLITE
                    return true
                  }
                  return false
                })
              }
              
              // 应用分组规则
              filteredStreams.forEach(stream => {
                stream.group = this.applyGroupRules(stream.name)
              })
              
              allResults.push(...filteredStreams)
            }
          } catch (error) {
            console.error(`采集地址失败 ${url}:`, error)
          }

          // 更新进度
          current++
          if (progressMessage) {
            progressMessage.message = `正在采集: ${current}/${total}`
          }

          // 每处理完一个URL，暂停一下
          await new Promise(resolve => setTimeout(resolve, 200))
        }

        // 关闭进度消息
        if (progressMessage) {
          progressMessage.close()
        }

        if (allResults.length > 0) {
          // 过滤重复项（同时考虑URL和分组）
          const existingKeys = new Set(this.streamList.map(s => `${s.url}|${s.group}`))
          const uniqueStreams = allResults.filter(s => {
            const key = `${s.url}|${s.group}`
            if (existingKeys.has(key)) {
              return false
            }
            existingKeys.add(key)
            return true
          })
          
          if (uniqueStreams.length > 0) {
            // 分批添加到列表，每批50个
            const addBatchSize = 50
            for (let i = 0; i < uniqueStreams.length; i += addBatchSize) {
              const batch = uniqueStreams.slice(i, i + addBatchSize)
              this.streamList.push(...batch)
              // 每添加一批，暂停一下
              await new Promise(resolve => setTimeout(resolve, 100))
            }
            
            this.$message.success(`成功采集 ${uniqueStreams.length} 个直播源`)
            this.saveStreamList()
          } else {
            this.$message.info('没有新的直播源可以导入')
          }
        } else {
          this.$message.warning('未找到有效的直播源')
        }
      } catch (error) {
        console.error('采集失败:', error)
        this.$message.error('采集失败: ' + error.message)
      } finally {
        if (progressMessage) {
          progressMessage.close()
        }
      }
    },

    // 添加带重试机制的采集方法
    async collectWithRetry(url, maxRetries = 3) {
      let lastError
      
      for (let i = 0; i < maxRetries; i++) {
        try {
          const response = await fetch(`${this.getBaseUrl()}/api/collect-url?url=${encodeURIComponent(url)}`, {
            method: 'GET',
            headers: {
              'Accept': 'text/plain'
            },
          })
          
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`)
          }
          
          const content = await response.text()
          const newStreams = this.parseContent(content)
          
          // 根据采集模式过滤直播源
          let filteredStreams = newStreams
          if (this.collectForm.mode === 'tv') {
            filteredStreams = newStreams.filter(s => {
              // 处理央视频道
              const isCCTV = /CCTV/i.test(s.name)
              if (isCCTV) {
                s.group = this.STANDARD_GROUPS.CCTV
                const cctvNumber = this.extractCCTVNumber(s.name)
                if (cctvNumber) {
                  s.name = cctvNumber
                }
                return true
              }
              // 处理卫视频道
              const isSatellite = /卫视/.test(s.name)
              if (isSatellite) {
                s.group = this.STANDARD_GROUPS.SATELLITE
                return true
              }
              return false
            })
          }
          
          // 再次确保所有CCTV频道都在正确分组
          filteredStreams.forEach(s => {
            if (/CCTV/i.test(s.name)) {
              s.group = this.STANDARD_GROUPS.CCTV
            }
          })
          
          return filteredStreams
          
        } catch (error) {
          lastError = error
          console.warn(`采集地址 ${url} 第 ${i + 1} 次尝试失败:`, error)
          
          if (i < maxRetries - 1) {
            continue // 直接进行下一次重试，不等待
          }
        }
      }
      
      // 所有重试都失败后抛出最后一次的错误
      throw new Error(`采集失败 [${url}]: ${lastError.message}`)
    },

    // 修改提取CCTV频道号的方法
    extractCCTVNumber(name) {
      // 尝试直接匹配CCTV+数字的形式
      const cctvMatch = name.match(/CCTV-?(\d+)(\+)?/i)
      if (cctvMatch) {
        const number = cctvMatch[1]
        const plus = cctvMatch[2] || ''
        return `CCTV${number}${plus}`
      }

      // 处理特殊频道名称（如CCTV5+）
      if (/CCTV5\+|CCTV5Plus|体育赛事/.test(name)) {
        return 'CCTV5+'
      }

      // 如果无法提取频道号，保留原始CCTV名称
      const cctvNameMatch = name.match(/CCTV[^a-z0-9]*([a-z0-9]+)/i)
      if (cctvNameMatch) {
        return `CCTV${cctvNameMatch[1]}`
      }

      // 如果都无法匹配，返回原始名称
      return name
    },

    async handleGenerateLink() {
      this.generateLinkDialogVisible = true
      try {
        // 先生成 TXT 格式链接
        const txtResponse = await fetch(`${this.getBaseUrl()}/api/generate-link`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-File-Format': 'txt'
          },
          body: this.generateContent('txt')
        })
        
        // 再生成 M3U 格式链接
        const m3uResponse = await fetch(`${this.getBaseUrl()}/api/generate-link`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-File-Format': 'm3u'
          },
          body: this.generateContent('m3u')
        })
        
        if (txtResponse.ok && m3uResponse.ok) {
          const txtLink = await txtResponse.text()
          const m3uLink = await m3uResponse.text()
          
          // 根据当前选择的格式显示对应的链接
          this.generatedLinks = {
            txt: txtLink,
            m3u: m3uLink
          }
          this.generatedLink = this.generatedLinks[this.linkForm.format]
        }
      } catch (error) {
        console.error('生成链接失败：', error)
        this.$message.error('生成链接失败')
      }
    },
    generateContent(format) {
      return format === 'txt' ? this.generateTxtContent() : this.generateM3uContent()
    },
    // 添加自定义排序方法
    customSort(streams, group) {
      return streams.sort((a, b) => {
        if (group === this.STANDARD_GROUPS.CCTV) {
          // 提取CCTV频道号进行排序
          const getNumber = (name) => {
            const match = name.match(/CCTV-?(\d+)(\+)?/i)
            if (match) {
              return parseInt(match[1]) + (match[2] ? 0.5 : 0) // CCTV5+ 会排在 CCTV5 后面
            }
            // 特殊处理CCTV5+
            if (/CCTV5\+|CCTV5Plus|体育赛事/.test(name)) {
              return 5.5
            }
            return 999 // 无法识别的排在最后
          }
          const numA = getNumber(a.name)
          const numB = getNumber(b.name)
          return numA - numB
        } else if (group === this.STANDARD_GROUPS.SATELLITE) {
          // 卫视按照拼音首字母排序
          return a.name.localeCompare(b.name, 'zh-CN')
        } else {
          // 其他分组按照默认排序
          return a.name.localeCompare(b.name)
        }
      })
    },
    generateTxtContent() {
      let content = ''
      const groupedStreams = {}
      
      // 首先整理所有流到对应的分组
      this.mergedStreams.forEach(stream => {
        const group = stream.group || '未分组'
        if (!groupedStreams[group]) {
          groupedStreams[group] = []
        }
        groupedStreams[group].push(stream)
      })

      // 优先处理央视和卫视分组
      const orderedGroups = [
        this.STANDARD_GROUPS.CCTV,
        this.STANDARD_GROUPS.SATELLITE,
        ...Object.keys(groupedStreams).filter(group => 
          group !== this.STANDARD_GROUPS.CCTV && 
          group !== this.STANDARD_GROUPS.SATELLITE
        )
      ]

      // 按分组生成内容
      orderedGroups.forEach(group => {
        if (groupedStreams[group] && groupedStreams[group].length > 0) {
          content += `${group},#genre#\n`
          // 使用自定义排序方法
          this.customSort(groupedStreams[group], group)
            .forEach(stream => {
        content += `${stream.name},${stream.urls.join('#')}\n`
            })
        }
      })
      
      return content.trim()
    },
    generateM3uContent() {
      let content = '#EXTM3U\n'
      const groupedStreams = {}
      
      // 首先整理所有流到对应的分组
      this.mergedStreams.forEach(stream => {
        const group = stream.group || '未分组'
        if (!groupedStreams[group]) {
          groupedStreams[group] = []
        }
        groupedStreams[group].push(stream)
      })

      // 优先处理央视和卫视分组
      const orderedGroups = [
        this.STANDARD_GROUPS.CCTV,
        this.STANDARD_GROUPS.SATELLITE,
        ...Object.keys(groupedStreams).filter(group => 
          group !== this.STANDARD_GROUPS.CCTV && 
          group !== this.STANDARD_GROUPS.SATELLITE
        )
      ]

      // 按分组生成内容
      orderedGroups.forEach(group => {
        if (groupedStreams[group] && groupedStreams[group].length > 0) {
          // 使用自定义排序方法
          this.customSort(groupedStreams[group], group)
            .forEach(stream => {
        stream.urls.forEach(url => {
                content += `#EXTINF:-1 group-title="${group}",${stream.name}\n${url}\n`
        })
            })
        }
      })
      
      return content.trim()
    },
    copyGeneratedLink() {
      const textarea = document.createElement('textarea')
      textarea.value = this.generatedLink
      document.body.appendChild(textarea)
      textarea.select()
      
      try {
        document.execCommand('copy')
        this.$message.success('链接已复制到剪贴板')
      } catch (err) {
        this.$message.error('复制失败')
      } finally {
        document.body.removeChild(textarea)
      }
    },
    downloadTxtFile() {
      const blob = new Blob([this.generatedLink], { type: 'text/plain;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `直播源列表_${new Date().toLocaleDateString()}.txt`
      
      document.body.appendChild(link)
      link.click()
      
      // 清理
      window.URL.revokeObjectURL(url)
      document.body.removeChild(link)
      this.$message.success('文件下载成功')
    },
    // 修改更新链接内容的方法
    async updateLinkContent() {
      try {
        // 更新 TXT 格式内容
        await fetch(`${this.getBaseUrl()}/api/update-content`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-File-Format': 'txt'
          },
          body: this.generateContent('txt')
        })
        
        // 更新 M3U 格式内容
        await fetch(`${this.getBaseUrl()}/api/update-content`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-File-Format': 'm3u'
          },
          body: this.generateContent('m3u')
        })
      } catch (error) {
        console.error('更新链接内容失败：', error)
      }
    },
    handleScheduleCheck() {
      if (this.isScheduled) {
        this.stopScheduleCheck()
      } else {
        this.scheduleDialogVisible = true
      }
    },
    startScheduleCheck() {
      this.$refs.scheduleForm.validate(valid => {
        if (valid) {
          this.scheduleDialogVisible = false
          this.isScheduled = true
          this.setupSchedule()
          this.saveState() // 保存状态
          this.$message.success('定时检测已开启')
        }
      })
    },
    stopScheduleCheck() {
      if (this.scheduleTimer) {
        clearInterval(this.scheduleTimer)
        this.scheduleTimer = null
      }
      this.isScheduled = false
      this.nextScheduleTime = null
      this.saveState() // 保存状态
      this.$message.success('定时检测已停止')
    },
    setupSchedule() {
      // 清除现有定时器
      if (this.scheduleTimer) {
        clearInterval(this.scheduleTimer)
      }

      const calculateNextExecutionTime = () => {
        const now = new Date()
        const [hours, minutes] = this.scheduleForm.executeTime.split(':').map(Number)
        let nextTime = new Date(now)
        nextTime.setHours(hours, minutes, 0, 0)

        // 如果当前时间已经过了今天的执行时间，设置为明天
        if (nextTime <= now) {
          nextTime.setDate(nextTime.getDate() + 1)
        }

        // 如果是每周模式，调整到下一个符合条件的星期
        if (this.scheduleForm.scheduleType === 'weekly') {
          const targetDay = parseInt(this.scheduleForm.weekDay)
          while (nextTime.getDay() !== targetDay) {
            nextTime.setDate(nextTime.getDate() + 1)
          }
        }

        return nextTime
      }

      const scheduleNextExecution = () => {
        const nextTime = calculateNextExecutionTime()
        const delay = nextTime.getTime() - new Date().getTime()

        // 更新下次执行时间显示
        this.nextScheduleTime = nextTime

        // 设置下次执行的定时器
        this.scheduleTimer = setTimeout(() => {
          // 如果当前正在检测中，等待检测完成后再执行
          if (!this.isChecking) {
            this.handleBatchCheck()
          }
          // 设置下一次执行
          scheduleNextExecution()
        }, delay)
      }

      // 开始第一次调度
      scheduleNextExecution()
    },
    // 保存当前状态到本地存储
    saveState() {
      try {
        // 清除旧的分块数据
        this.clearStorageData()

        // 只保存必要的数据字段，减少数据量
        const minimalStreamList = this.streamList.map(stream => ({
          name: stream.name,
          url: stream.url,
          group: stream.group,
          status: stream.status,
          resolution: stream.resolution || '',
          responseTime: stream.responseTime || 0
        }))

        // 将streamList分块存储，减小分块大小
        const streamListStr = JSON.stringify(minimalStreamList)
        const chunkSize = 100000 // 减小到100KB
        const chunks = Math.ceil(streamListStr.length / chunkSize)
        
        // 如果数据量太大，只保存基本信息
        if (chunks > 10) {
          console.warn('数据量过大，只保存基本状态信息')
          const basicState = {
            currentPage: this.currentPage,
            pageSize: this.pageSize,
            scheduleForm: this.scheduleForm,
            collectForm: this.collectForm
          }
          localStorage.setItem(this.stateKey, JSON.stringify(basicState))
          return
        }

        // 分块存储
        for (let i = 0; i < chunks; i++) {
          const chunk = streamListStr.slice(i * chunkSize, (i + 1) * chunkSize)
          try {
            localStorage.setItem(`${this.stateKey}_streamList_${i}`, chunk)
          } catch (e) {
            console.error('存储分块失败:', e)
            this.clearStorageData()
            // 尝试只保存基本信息
            const basicState = {
              currentPage: this.currentPage,
              pageSize: this.pageSize,
              scheduleForm: this.scheduleForm,
              collectForm: this.collectForm
            }
            localStorage.setItem(this.stateKey, JSON.stringify(basicState))
            return
          }
        }

        // 存储其他状态数据
        const state = {
          currentPage: this.currentPage,
          pageSize: this.pageSize,
          scheduleForm: this.scheduleForm,
          collectForm: this.collectForm,
          streamListChunks: chunks
        }
        localStorage.setItem(this.stateKey, JSON.stringify(state))
      } catch (error) {
        console.error('保存状态失败:', error)
        this.clearStorageData()
        // 尝试只保存基本信息
        try {
          const basicState = {
            currentPage: this.currentPage,
            pageSize: this.pageSize
          }
          localStorage.setItem(this.stateKey, JSON.stringify(basicState))
        } catch (e) {
          console.error('保存基本状态也失败:', e)
        }
      }
    },

    // 从本地存储恢复状态
    restoreState() {
      try {
        const savedState = localStorage.getItem(this.stateKey)
        if (savedState) {
          const state = JSON.parse(savedState)
          
          // 恢复streamList
          if (state.streamListChunks) {
            try {
              let streamListStr = ''
              for (let i = 0; i < state.streamListChunks; i++) {
                const chunk = localStorage.getItem(`${this.stateKey}_streamList_${i}`)
                if (chunk) {
                  streamListStr += chunk
                }
              }
              if (streamListStr) {
                this.streamList = JSON.parse(streamListStr)
              }
            } catch (e) {
              console.error('恢复streamList失败:', e)
              this.streamList = []
            }
          }
          
          // 恢复其他状态
          this.currentPage = state.currentPage || 1
          this.pageSize = state.pageSize || 5
          if (state.scheduleForm) this.scheduleForm = state.scheduleForm
          if (state.collectForm) this.collectForm = state.collectForm
        }

        // 重置所有运行状态
        this.isChecking = false
        this.isPaused = false
        this.isScheduled = false
        this.isCollecting = false
        this.checkedCount = 0
        this.totalCount = 0
        this.pausedIndex = -1
        this.nextScheduleTime = null
      } catch (error) {
        console.error('恢复状态失败:', error)
        this.clearStorageData()
        this.streamList = []
      }
    },

    // 清理存储数据
    clearStorageData() {
      try {
        localStorage.removeItem(this.stateKey)
        for (let i = 0; ; i++) {
          const key = `${this.stateKey}_streamList_${i}`
          if (localStorage.getItem(key) === null) {
            break
          }
          localStorage.removeItem(key)
        }
      } catch (error) {
        console.error('清理存储数据失败:', error)
      }
    },
    // 添加重置所有运行状态的方法
    resetAllStates() {
      // 停止定时检测
      if (this.isScheduled) {
        this.stopScheduleCheck()
      }
      
      // 停止采集
      if (this.isCollecting) {
        this.stopCollecting()
      }
      
      // 停止批量检测
      if (this.isChecking) {
        this.shouldStop = true
        this.isChecking = false
        this.isPaused = false
      }
      
      // 重置所有状态
      this.isChecking = false
      this.isPaused = false
      this.isScheduled = false
      this.isCollecting = false
      this.checkedCount = 0
      this.totalCount = 0
      this.pausedIndex = -1
      this.nextScheduleTime = null
      
      // 清除所有定时器
      if (this.scheduleTimer) {
        clearInterval(this.scheduleTimer)
        this.scheduleTimer = null
      }
      if (this.collectTimer) {
        clearInterval(this.collectTimer)
        this.collectTimer = null
      }
      
      // 保存重置后的状态
      this.saveState()
      
      this.$message.success('所有运行状态已重置')
    },
    // 更新活动状态
    updateActiveStatus() {
      const hasActive = this.isChecking || this.isCollecting || this.isScheduled
      
      if (this.hasActiveOperations !== hasActive) {
        this.hasActiveOperations = hasActive
        if (hasActive) {
          // 添加刷新拦截
          window.addEventListener('beforeunload', this.handleBeforeUnload)
        } else {
          // 移除刷新拦截
          window.removeEventListener('beforeunload', this.handleBeforeUnload)
        }
      }
    },
    // 处理页面刷新
    handleBeforeUnload(e) {
      if (this.hasActiveOperations) {
        const message = '有正在执行的操作，确定要离开吗？'
        e.preventDefault()
        e.returnValue = message
        return message
      }
    },
    addCollectUrl() {
      const newIndex = this.collectForm.urls.length
        this.collectForm.urls.push('')
      // 初始化新URL的状态
      this.$set(this.collectForm.urlStatus, newIndex, {
        isChecking: false,
        isAccessible: null
      })
    },
    removeCollectUrl(index) {
      if (Array.isArray(this.collectForm.urls) && this.collectForm.urls.length > 1) {
        this.collectForm.urls.splice(index, 1)
        
        // 重新排序urlStatus
        const newUrlStatus = {}
        Object.keys(this.collectForm.urlStatus || {})
          .filter(key => parseInt(key) !== index)
          .forEach((key, i) => {
            newUrlStatus[i] = this.collectForm.urlStatus[key]
          })
        this.collectForm.urlStatus = newUrlStatus
      }
    },
    // 添加判断分辨率标签类型的方法
    getResolutionTagType(resolution) {
      if (!resolution || resolution === '-') return 'info'
      if (resolution === '多播源') return 'warning'  // 为多播源添加特殊样式
      const [width] = resolution.split('x').map(Number)
      if (width >= 1920) return 'success'
      if (width >= 1280) return ''  // 默认类型
      return 'warning'
    },
    // 添加判断响应时间标签类型的方法
    getResponseTimeTagType(responseTime) {
      if (!responseTime || responseTime === '-') return 'info'
      const time = Number(responseTime)
      if (time <= 1000) return 'success'
      if (time <= 2000) return ''  // 默认类型
      if (time <= 3000) return 'warning'
      return 'danger'
    },
    setupCollectSchedule() {
      // 清除现有定时器
      if (this.collectTimer) {
        clearInterval(this.collectTimer)
      }

      const calculateNextCollectTime = () => {
        const now = new Date()
        const [hours, minutes] = this.collectForm.executeTime.split(':').map(Number)
        let nextTime = new Date(now)
        nextTime.setHours(hours, minutes, 0, 0)

        // 如果当前时间已经过了今天的执行时间，设置为明天
        if (nextTime <= now) {
          nextTime.setDate(nextTime.getDate() + 1)
        }

        // 如果是每周模式，调整到下一个符合条件的星期
        if (this.collectForm.collectType === 'weekly') {
          const targetDay = parseInt(this.collectForm.weekDay)
          while (nextTime.getDay() !== targetDay) {
            nextTime.setDate(nextTime.getDate() + 1)
          }
        }

        return nextTime
      }

      const scheduleNextCollection = () => {
        const nextTime = calculateNextCollectTime()
        const delay = nextTime.getTime() - new Date().getTime()

        // 设置下次执行的定时器
        this.collectTimer = setTimeout(() => {
          // 执行采集
          this.collect()
          // 设置下一次执行
          scheduleNextCollection()
        }, delay)
      }

      // 开始第一次调度
      scheduleNextCollection()
    },
    handleSearch() {
      this.filteredList = this.streamList.filter(item => {
        // 对CCTV频道进行精确匹配
        if (this.searchForm.name && /^cctv\d*$/i.test(this.searchForm.name)) {
          const searchPattern = new RegExp(`^${this.searchForm.name}[\\s-]*$`, 'i');
          const nameMatch = searchPattern.test(item.name.toLowerCase().replace(/[^a-z0-9]/gi, ''));
          const groupMatch = !this.searchForm.group || item.group === this.searchForm.group;
          const statusMatch = !this.searchForm.status || item.status === this.searchForm.status;
          return nameMatch && groupMatch && statusMatch;
        }
        
        // 对其他搜索使用常规匹配
        const nameMatch = !this.searchForm.name || 
          item.name.toLowerCase().includes(this.searchForm.name.toLowerCase());
        const groupMatch = !this.searchForm.group || 
          item.group === this.searchForm.group;
        const statusMatch = !this.searchForm.status || 
          item.status === this.searchForm.status;
        
        return nameMatch && groupMatch && statusMatch;
      });
      
      // 重置到第一页
      this.currentPage = 1;
      this.searchDialogVisible = false;
    },
    resetSearch() {
      this.searchForm = {
        name: '',
        group: '',
        status: ''
      }
      this.filteredList = []
      this.currentPage = 1
    },
    // 获取真实IP地址
    async getRealIp() {
      try {
        const response = await fetch(`${this.getBaseUrl()}/api/local-ip`)
        if (response.ok) {
          const data = await response.json()
          this.realIp = data.ip
          localStorage.setItem('realIp', data.ip)  // 保存真实IP到localStorage
        }
      } catch (error) {
        console.error('获取IP失败:', error)
      }
    },
    // 获取基础URL
    getBaseUrl() {
      const serverIp = localStorage.getItem('serverIp')
      if (serverIp) {
        return `http://${serverIp}:8080`
      }
      return 'http://localhost:8080'
    },
    async saveGithubSettings() {
      try {
        await this.$refs.syncForm.validate()

        // 保存设置到localStorage
        localStorage.setItem('githubToken', this.syncForm.token)
        localStorage.setItem('githubRepo', this.syncForm.repo)
        localStorage.setItem('githubTxtPath', this.syncForm.txtPath)
        localStorage.setItem('githubM3uPath', this.syncForm.m3uPath)
        localStorage.setItem('githubAutoSync', this.syncForm.autoSync)
        localStorage.setItem('githubSyncType', this.syncForm.syncType)
        localStorage.setItem('githubExecuteTime', this.syncForm.executeTime)
        localStorage.setItem('githubWeekDay', this.syncForm.weekDay)

        // 如果开启了自动同步，重新设置定时器
        if (this.syncForm.autoSync) {
          this.setupSyncSchedule()
        } else {
          this.clearSyncSchedule()
        }

        this.$message.success('设置已保存')
        this.syncDialogVisible = false
      } catch (error) {
        this.$message.error(error.message || '保存失败')
      }
    },
    async syncToGithub(content, path) {
      try {
        // 首先获取文件的SHA
        const getResponse = await fetch(`https://api.github.com/repos/${this.syncForm.repo}/contents/${path}`, {
          headers: {
            'Authorization': `token ${this.syncForm.token}`
          }
        })

        if (!getResponse.ok) {
          throw new Error(`获取文件信息失败: ${getResponse.status} ${getResponse.statusText}`)
        }

        const fileInfo = await getResponse.json()
        const sha = fileInfo.sha

        // 将内容转换为UTF-8编码的Base64
        const base64Content = btoa(unescape(encodeURIComponent(content)))

        // 更新文件内容
        const updateResponse = await fetch(`https://api.github.com/repos/${this.syncForm.repo}/contents/${path}`, {
          method: 'PUT',
          headers: {
            'Authorization': `token ${this.syncForm.token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            message: this.syncForm.message || '更新直播源列表',
            content: base64Content,
            sha: sha
          })
        })

        if (!updateResponse.ok) {
          throw new Error(`更新文件失败: ${updateResponse.status} ${updateResponse.statusText}`)
        }

        return await updateResponse.json()
      } catch (error) {
        console.error('同步到GitHub失败:', error)
        throw new Error('同步失败: ' + error.message)
      }
    },
    handleAutoSyncChange(value) {
      if (value) {
        this.setupSyncSchedule()
      } else {
        this.clearSyncSchedule()
      }
    },
    setupSyncSchedule() {
      this.clearSyncSchedule()
      const calculateNextSyncTime = () => {
        const now = new Date()
        const [hours, minutes] = this.syncForm.executeTime.split(':').map(Number)
        let nextTime = new Date(now)
        nextTime.setHours(hours, minutes, 0, 0)

        // 如果当前时间已经过了今天的执行时间，设置为明天
        if (nextTime <= now) {
          nextTime.setDate(nextTime.getDate() + 1)
        }

        // 如果是每周模式，调整到下一个符合条件的星期
        if (this.syncForm.syncType === 'weekly') {
          const targetDay = parseInt(this.syncForm.weekDay)
          while (nextTime.getDay() !== targetDay) {
            nextTime.setDate(nextTime.getDate() + 1)
          }
        }

        return nextTime
      }

      const scheduleNextSync = () => {
        const nextTime = calculateNextSyncTime()
        const delay = nextTime.getTime() - new Date().getTime()

        // 设置下次执行的定时器
        this.syncTimer = setTimeout(async () => {
          try {
            // 执行同步
            await this.syncToGithub(this.generateContent('txt'), this.syncForm.txtPath)
            await this.syncToGithub(this.generateContent('m3u'), this.syncForm.m3uPath)
            this.$message.success('已同步到GitHub')
          } catch (error) {
            this.$message.error('同步失败：' + error.message)
          }
          // 设置下一次执行
          scheduleNextSync()
        }, delay)
      }

      // 开始第一次调度
      scheduleNextSync()
    },
    clearSyncSchedule() {
      if (this.syncTimer) {
        clearInterval(this.syncTimer)
        this.syncTimer = null
      }
    },
    getStatusTagType(status) {
      switch (status) {
        case '正常':
          return 'success'
        case '异常':
          return 'danger'
        case '未知':
          return 'warning'
        case '未检测':
          return 'info'
        default:
          return 'info'
      }
    },
    // 修改显示消息的方法
    showMessage(content) {
      // 如果是清除直播源的消息，使用新的消息系统
      if (content.includes('已自动清除直播源') || content.includes('(')) {
        const id = this.messageId++
        const message = {
          id,
          content: content.includes('(') ? content : content.replace('已自动清除直播源：', '')
        }
        this.messages.push(message)
        
        // 3秒后自动移除消息
        setTimeout(() => {
          this.removeMessage(id)
        }, 3000)
        
        // 最多显示3条消息
        if (this.messages.length > 3) {
          this.messages.shift()
        }
      } else {
        // 其他消息使用 Element UI 的消息提示
        this.$message.info(content)
      }
    },
    
    // 移除消息
    removeMessage(id) {
      const index = this.messages.findIndex(msg => msg.id === id)
      if (index !== -1) {
        this.messages.splice(index, 1)
      }
    },
    // 修改清除直播源的提示
    handleStreamRemoval(stream, reason) {
      this.showMessage(`${stream.name} (${reason})`)
    },
    async checkUrlAccessibility() {
      if (!this.importUrlForm.url) return
      
      this.importUrlForm.isChecking = true
      this.importUrlForm.isAccessible = null
      
      try {
        // 添加延迟，避免频繁请求
        await new Promise(resolve => setTimeout(resolve, 500))
        
        const response = await fetch(`${this.getBaseUrl()}/api/check-url`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            url: this.importUrlForm.url
          })
        })
        
        if (!response.ok) throw new Error('请求失败')
        const result = await response.json()
        this.importUrlForm.isAccessible = result.accessible
      } catch (error) {
        console.error('检查链接可达性失败:', error)
        this.importUrlForm.isAccessible = false
      } finally {
        this.importUrlForm.isChecking = false
      }
    },
    async handleImportUrl() {
      if (!this.importUrlForm.url) {
        this.$message.warning('请输入文件链接')
        return
      }

      // 如果链接未经过检测或检测失败，先进行检测
      if (this.importUrlForm.isAccessible === null || this.importUrlForm.isAccessible === false) {
        await this.checkUrlAccessibility()
        if (!this.importUrlForm.isAccessible) {
          return
        }
      }

      try {
        // 获取文件内容
        const response = await fetch(this.importUrlForm.url)
        if (!response.ok) throw new Error('获取文件失败')
        
        const content = await response.text()
        const newStreams = this.parseContent(content)
        
        if (newStreams.length > 0) {
          // 应用分组规则
          newStreams.forEach(stream => {
            stream.group = this.applyGroupRules(stream.name)
          })

          // 提取URL列表并过滤重复项（同时考虑URL和分组）
          const existingKeys = new Set(this.streamList.map(s => `${s.url}|${s.group}`))
          const uniqueStreams = newStreams.filter(s => {
            const key = `${s.url}|${s.group}`
            if (existingKeys.has(key)) {
              return false
            }
            existingKeys.add(key)
            return true
          })
          
          if (uniqueStreams.length > 0) {
            // 批量添加到列表
            this.streamList.push(...uniqueStreams)
            this.$message.success(`成功导入 ${uniqueStreams.length} 个直播源`)
            this.importUrlDialogVisible = false
          } else {
            this.$message.info('没有新的直播源可以导入')
          }
        } else {
          this.$message.warning('未找到有效的直播源')
        }
      } catch (error) {
        console.error('导入失败:', error)
        this.$message.error('导入失败: ' + error.message)
      } finally {
        this.importUrlForm.isChecking = false
      }
    },
    getPlaceholderText() {
      switch (this.collectForm.mode) {
        case 'tv':
          return '请输入包含央视或卫视频道的网络地址'
        default:
          return '请输入需要采集的网络地址'
      }
    },
    async handleCollectUrlsFileUpload(event) {
      const file = event.target.files[0]
      if (!file) return

      try {
        const content = await this.readFile(file)
        // 按行分割并过滤空行
        const urls = content.split('\n')
          .map(line => line.trim())
          .filter(line => line && line.length > 0)
        
        if (urls.length === 0) {
          this.$message.warning('文件中未找到有效的URL地址')
          return
        }

        // 验证URL格式
        const invalidUrls = urls.filter(url => !/^https?:\/\/.+/.test(url))
        if (invalidUrls.length > 0) {
          this.$message.warning(`发现 ${invalidUrls.length} 个无效的URL地址`)
          return
        }

        // 初始化状态对象
        const urlStatus = {}
        urls.forEach((_, index) => {
          urlStatus[index] = {
            isChecking: false,
            isAccessible: null
          }
        })
        
        // 使用Vue的响应式更新
        this.$set(this.collectForm, 'urls', urls)
        this.$set(this.collectForm, 'urlStatus', urlStatus)
        
        // 手动触发每个URL的检测
        this.$nextTick(() => {
          urls.forEach((url, index) => {
            this.checkCollectUrlAccessibility(url, index)
          })
        })

        this.$message.success(`成功导入 ${urls.length} 个采集地址`)
      } catch (error) {
        this.$message.error('读取文件失败：' + error.message)
      } finally {
        // 清除文件输入，允许重复选择同一文件
        event.target.value = ''
      }
    },
    async checkCollectUrlAccessibility(url, index) {
      if (!url) {
        console.warn('URL为空，跳过检测')
        this.$set(this.collectForm.urlStatus, index, {
          isChecking: false,
          isAccessible: null,
          error: '地址为空'
        })
        return
      }

      // 标准化URL格式
      let normalizedUrl = url
      if (!url.toLowerCase().startsWith('http')) {
        normalizedUrl = 'http://' + url
      }
      
      console.log(`开始检测URL (${index}):`, normalizedUrl)
      
      // 设置检查状态
      this.$set(this.collectForm.urlStatus, index, {
        isChecking: true,
        isAccessible: null,
        error: null
      })
      
      try {
        // 添加延迟，避免频繁请求
        await new Promise(resolve => setTimeout(resolve, 500))
        
        const response = await fetch(`${this.getBaseUrl()}/api/check-url`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ url: normalizedUrl })
        })
        
        if (!response.ok) {
          throw new Error(`HTTP错误: ${response.status}`)
        }

        const result = await response.json()
        console.log(`URL (${index}) 检测结果:`, result)
        
        // 更新状态
        this.$set(this.collectForm.urlStatus, index, {
          isChecking: false,
          isAccessible: result.accessible,
          statusCode: result.statusCode,
          error: result.error
        })

        // 如果检测失败，记录错误信息
        if (!result.accessible) {
          console.warn(`URL (${index}) 不可访问:`, result.error || '未知错误')
        }
      } catch (error) {
        console.error(`URL (${index}) 检测失败:`, error)
        this.$set(this.collectForm.urlStatus, index, {
          isChecking: false,
          isAccessible: false,
          error: error.message || '检测失败'
        })
      }
    },
    clearCollectUrls() {
      this.$confirm('确认清空所有采集地址吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 重置采集地址列表
        this.$set(this.collectForm, 'urls', [''])
        // 重置状态对象
        this.$set(this.collectForm, 'urlStatus', {
          0: {
            isChecking: false,
            isAccessible: null
          }
        })
        this.$message.success('已清空采集地址')
      }).catch(() => {
        // 用户取消操作
      })
    },
    async importFromGithub() {
      if (!this.syncForm.token || !this.syncForm.repo || !this.syncForm.txtPath) {
        this.$message.warning('请先配置GitHub同步设置')
        return
      }

      this.$message.info('正在从GitHub导入...')
      try {
        // 使用GitHub API获取文件内容
        const response = await fetch(`https://api.github.com/repos/${this.syncForm.repo}/contents/${this.syncForm.txtPath}`, {
          headers: {
            'Authorization': `token ${this.syncForm.token}`,
            'Accept': 'application/vnd.github.v3.raw'
          }
        })

        if (!response.ok) {
          throw new Error(`GitHub API请求失败: ${response.status} ${response.statusText}`)
        }

        const content = await response.text()
        if (!content) {
          throw new Error('获取到的内容为空')
        }

        // 解析内容
        const lines = content.split('\n')
        const newStreams = []
        let currentGroup = '未分组'
        let currentStream = null

        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine || trimmedLine.startsWith('#')) continue

          if (trimmedLine.startsWith('[')) {
            // 这是一个分组
            currentGroup = trimmedLine.replace(/[[\]]/g, '').trim()
          } else if (trimmedLine.includes(',')) {
            // 这是一个直播源
            const [name, url] = trimmedLine.split(',').map(s => s.trim())
            if (name && url) {
              currentStream = {
                name,
                url,
                group: currentGroup,
                status: '待检测',
                resolution: '',
                responseTime: 0,
                lastCheck: '',
                isSelected: false
              }
              newStreams.push(currentStream)
            }
          }
        }

        if (newStreams.length === 0) {
          throw new Error('未找到有效的直播源')
        }

        // 更新列表
        this.streamList = newStreams
        this.filteredList = [...newStreams]
        this.currentPage = 1
        this.total = newStreams.length

        // 保存到localStorage
        localStorage.setItem('streamList', JSON.stringify(newStreams))
        localStorage.setItem('filteredList', JSON.stringify(newStreams))

        this.$message.success(`成功导入 ${newStreams.length} 个直播源`)
        this.importDialogVisible = false
      } catch (error) {
        console.error('导入失败:', error)
        this.$message.error('导入失败: ' + error.message)
      }
    },
    saveGroupSettings() {
      // 保存分组规则到localStorage
      localStorage.setItem('groupSettings', JSON.stringify(this.groupSettingsForm))
      
      // 重新应用分组规则到所有直播源
      this.streamList.forEach(stream => {
        stream.group = this.applyGroupRules(stream.name)
      })
      
      this.$message.success('分组规则已保存并应用')
      this.groupSettingsDialogVisible = false
      
      // 保存更新后的列表
      this.saveStreamList()
    },
    applyGroupRules(name) {
      if (!name) return '未分组'

      // 解析分组规则
      const rules = this.groupSettingsForm.rules.split('\n').filter(rule => rule.trim())
      for (const rule of rules) {
        if (!rule.includes(':')) continue
        
        const [groupName, keywords] = rule.split(':')
        if (!keywords) continue
        
        const patterns = keywords.split('#').filter(k => k.trim())
        if (patterns.length === 0) continue
        
        try {
          if (new RegExp(patterns.join('|'), 'i').test(name)) {
            return groupName.trim()
          }
        } catch (e) {
          console.error('分组规则格式错误:', e)
        }
      }
      
      return '未分组'
    },
  },
  watch: {
    // 只监听数据变化
    streamList: {
      handler() {
        this.updateLinkContent()
        this.saveState()
      },
      deep: true
    },
    currentPage() {
      this.saveState()
    },
    pageSize() {
      this.saveState()
    },
    // 监听所有可能的运行状态
    isChecking() {
      this.updateActiveStatus()
    },
    isCollecting() {
      this.updateActiveStatus()
    },
    isScheduled() {
      this.updateActiveStatus()
    },
    // 添加格式变化监听
    'linkForm.format'() {
      if (this.generateLinkDialogVisible) {
        this.handleGenerateLink()
      }
    }
  },
  // 组件被激活时（重新显示）
  activated() {
    // 如果有定时任务，恢复定时器
    if (this.isScheduled) {
      this.setupSchedule()
    }
    // 如果有采集任务，且不是从其他页面切换回来，才恢复采集
    if (this.isCollecting && !this.isPageSwitching) {
      this.startCollecting(true)
    }
    // 如果有GitHub自动同步，恢复同步定时器
    if (this.syncForm.autoSync) {
      this.setupSyncSchedule()
    }
    // 重置页面切换标志
    this.isPageSwitching = false
  },
  // 组件被停用时（离开但不销毁）
  deactivated() {
    // 设置页面切换标志
    this.isPageSwitching = true
    // 暂停定时器，但保持状态标志
    if (this.scheduleTimer) {
      clearInterval(this.scheduleTimer)
      this.scheduleTimer = null
    }
    // 暂停采集定时器，但保持状态标志
    if (this.collectTimer) {
      clearInterval(this.collectTimer)
      this.collectTimer = null
    }
    // 暂停同步定时器，但保持状态标志
    if (this.syncTimer) {
      clearInterval(this.syncTimer)
      this.syncTimer = null
    }
  }
}
</script>

<style scoped>
.stream-check {
  padding: 25px;
  background-color: #fff;
  border-radius: 4px;
  height: calc(100vh - 130px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.table-operations {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.button-groups {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.button-group {
  display: flex;
  align-items: center;
}

/* 确保按钮组在小屏幕上不会被压缩 */
:deep(.el-button-group) {
  white-space: nowrap;
  margin: 0;
}

/* 确保按钮内容不会换行 */
:deep(.el-button) {
  white-space: nowrap;
}

/* 在较小屏幕上调整按钮大小 */
@media screen and (max-width: 768px) {
  :deep(.el-button) {
    padding: 8px 15px;
    font-size: 12px;
  }
  
  :deep(.el-button [class*="el-icon-"]) {
    font-size: 14px;
  }
}

/* 在更小的屏幕上进一步调整 */
@media screen and (max-width: 576px) {
  .button-groups {
    gap: 5px;
  }
  
  :deep(.el-button) {
    padding: 6px 10px;
    font-size: 12px;
  }
}

/* 添加表格容器样式 */
.el-table-wrapper {
  flex: 1;
  position: relative; /* 添加相对定位 */
  min-height: 200px; /* 设置最小高度 */
}

/* 覆盖 element-ui 的表格样式 */
:deep(.el-table) {
  position: absolute; /* 绝对定位 */
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  height: 100%;
  max-height: 100%;
}

:deep(.el-table__body-wrapper) {
  height: calc(100% - 40px) !important; /* 减去表头高度 */
  overflow-y: auto !important;
}

:deep(.el-table__fixed-right) {
  height: 100% !important; /* 修复固定列的高度 */
  bottom: 0px;
}

:deep(.el-table__fixed-right .el-table__fixed-body-wrapper) {
  height: calc(100% - 40px) !important; /* 减去表头高度 */
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  flex-shrink: 0; /* 防止分页被压缩 */
  padding-bottom: 10px; /* 添加底部间距 */
  background-color: #fff; /* 确保分页器背景色 */
  position: relative; /* 添加相对定位 */
  z-index: 1; /* 确保分页器在最上层 */
}

.el-button [class*="el-icon-"] {
  font-size: 18px;
}

.delete-btn {
  color: #F56C6C;
}

.delete-btn:hover {
  color: #ff4d4d;
}

.setting-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

.link-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.link-info p {
  margin: 0;
  color: #606266;
}

.link-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 10px;
}

/* 生成链接对话框样式 */
.generate-link-dialog {
  max-width: 600px;
}

.link-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.link-card, .tips-card {
  margin: 0;
  border: 1px solid #EBEEF5;
}

.link-content {
  padding: 10px;
}

.tips-content {
  color: #606266;
  font-size: 14px;
  padding: 10px;
}

.tips-content ul {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

/* 覆盖 element-ui 的一些默认样式 */
:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-card__header) {
  padding: 12px 20px;
  font-weight: 500;
}

:deep(.el-card__body) {
  padding: 15px;
}

/* 可以添加一些自定义样式 */
.el-tag {
  width: 90px;  /* 统一标签宽度 */
}

.el-tag + .el-tag {
  margin-left: 4px;
}

/* 修改消息面板样式 */
.message-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 300px;
  z-index: 9999;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}

.message-panel.has-messages {
  opacity: 1;
}

.message-item {
  background: rgba(64, 158, 255, 0.95);
  color: white;
  border-radius: 4px;
  padding: 10px 15px;
  margin-bottom: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
  display: flex;
  align-items: center;
  font-size: 14px;
  transition: all 0.3s;
}

.message-item i {
  margin-right: 8px;
  font-size: 16px;
}

.message-content {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 消息动画 */
.message-list-enter-active, .message-list-leave-active {
  transition: all 0.3s;
}

.message-list-enter {
  transform: translateY(100%);
  opacity: 0;
}

.message-list-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.group-settings-help {
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.group-settings-help p {
  margin: 5px 0;
}
</style> 