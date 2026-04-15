<template>
  <div class="app-container">
    <div class="search-container">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.keyword"
            placeholder="订单号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="订单状态" style="width: 150px;">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="待确认" :value="1" />
            <!-- <el-option label="已确认" :value="2" /> -->
            <el-option label="已支付" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>

        <el-form-item label="婚礼日期">
          <el-date-picker
            v-model="queryParams.weddingDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
          <el-button icon="refresh" @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="pageData" border stripe>
        <el-table-column label="订单号" prop="orderNo" width="150" />
        <el-table-column label="新人信息" width="150">
          <template #default="scope">
            {{ scope.row.userName }}
          </template>
        </el-table-column>
        <el-table-column label="主持人" width="120">
          <template #default="scope">
            {{ scope.row.hostName }}
          </template>
        </el-table-column>
        <el-table-column label="婚礼日期" prop="weddingDate" width="120" />
        <el-table-column label="婚礼类型" width="120">
          <template #default="scope">
            {{ getWeddingTypeText(scope.row.weddingType) }}
          </template>
        </el-table-column>
        <el-table-column label="订单金额" prop="amount" width="100">
          <template #default="scope">
            ¥{{ scope.row.amount }}
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="150" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="scope">
            <el-button type="primary" icon="view" link size="small" @click="handleOpenDialog(scope.row.id, scope.row.status)">
              {{ isOrderFinished(scope.row.status) ? '查看' : '编辑' }}
            </el-button>
            <!-- <el-button
              v-if="!isOrderFinished(scope.row.status)"
              type="danger"
              icon="delete"
              link
              size="small"
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button> -->
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="fetchData"
      />
    </el-card>

    <!-- 表单弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="600px" @close="handleCloseDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px" :disabled="isViewMode">
        <!-- <el-form-item label="用户ID" prop="userId">
          <el-input-number v-model="formData.userId" :min="1" />
        </el-form-item>

        <el-form-item label="主持人ID" prop="hostId">
          <el-input-number v-model="formData.hostId" :min="1" />
        </el-form-item> -->

        <el-form-item label="婚礼日期" prop="weddingDate">
          <el-date-picker v-model="formData.weddingDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>

        <el-form-item label="婚礼类型" prop="weddingType">
          <el-radio-group v-model="formData.weddingType">
            <el-radio label="01">中式婚礼</el-radio>
            <el-radio label="02">西式婚礼</el-radio>
            <el-radio label="03">主题婚礼</el-radio>
            <el-radio label="04">户外婚礼</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="订单金额" prop="amount">
          <el-input-number v-model="formData.amount" :min="0" :precision="2" />
        </el-form-item>

        <el-form-item label="订单状态" prop="status">
          <el-select v-model="formData.status" :disabled="isStatusDisabled">
            <el-option label="待确认" :value="1" />
            <!-- <el-option label="已确认" :value="2" /> -->
            <el-option label="已支付" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
          <span v-if="isStatusDisabled" class="status-tip">（已完成或已取消的订单不能更改状态）</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleCloseDialog">{{ isViewMode ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isViewMode" type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderPage, getOrderById, saveOrder, updateOrder, deleteOrder } from '@/api/marrylink-api'

const queryParams = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: null,
  weddingDate: null
})

const pageData = ref([])
const total = ref(0)
const loading = ref(false)

const dialog = reactive({
  visible: false,
  title: '创建订单'
})

const formData = reactive({
  id: null,
  userId: null,
  hostId: null,
  weddingDate: '',
  weddingType: '',
  amount: 0,
  status: 1
})

const formRef = ref()

// 原始状态，用于判断是否可以更改状态
const originalStatus = ref(null)

// 是否为查看模式（已完成或已取消的订单只能查看）
const isViewMode = ref(false)

// 判断订单是否已完成或已取消
function isOrderFinished(status) {
  return status === 4 || status === 5
}

// 计算属性：判断状态是否禁用（已完成=4 或 已取消=5）
const isStatusDisabled = computed(() => {
  return originalStatus.value === 4 || originalStatus.value === 5
})

const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  hostId: [{ required: true, message: '请输入主持人ID', trigger: 'blur' }],
  weddingDate: [{ required: true, message: '请选择婚礼日期', trigger: 'change' }]
}

function getStatusType(status) {
  const types = { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success', 5: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 1: '待确认', 3: '已支付', 4: '已完成', 5: '已取消' }
  return texts[status] || '未知'
}

function getWeddingTypeText(type) {
  const types = { '01': '中式婚礼', '02': '西式婚礼', '03': '主题婚礼', '04': '户外婚礼' }
  return types[type] || type
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderPage(queryParams)
    pageData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  fetchData()
}

function handleResetQuery() {
  queryParams.keyword = ''
  queryParams.status = null
  queryParams.weddingDate = null
  handleQuery()
}

async function handleOpenDialog(id, status) {
  dialog.visible = true
  if (id) {
    const res = await getOrderById(id)
    Object.assign(formData, res)
    // 保存原始状态
    originalStatus.value = res.status
    // 判断是否为查看模式
    if (isOrderFinished(res.status)) {
      dialog.title = '查看订单'
      isViewMode.value = true
    } else {
      dialog.title = '编辑订单'
      isViewMode.value = false
    }
  } else {
    dialog.title = '创建订单'
    originalStatus.value = null
    isViewMode.value = false
  }
}

function handleCloseDialog() {
  dialog.visible = false
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    userId: null,
    hostId: null,
    weddingDate: '',
    weddingType: '',
    amount: 0,
    status: 1
  })
  // 重置原始状态
  originalStatus.value = null
  // 重置查看模式
  isViewMode.value = false
}

function handleSubmit() {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (formData.id) {
          await updateOrder(formData)
          ElMessage.success('修改成功')
        } else {
          await saveOrder(formData)
          ElMessage.success('创建成功')
        }
        handleCloseDialog()
        fetchData()
      } finally {
        loading.value = false
      }
    }
  })
}

function handleDelete(id) {
  ElMessageBox.confirm('确认删除该订单?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteOrder(id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>