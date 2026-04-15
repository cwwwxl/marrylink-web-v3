<template>
  <div class="app-container">
    <div class="search-container">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="订单号"
            clearable
            @keyup.enter="handleQuery"
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
        <el-table-column label="订单号" prop="orderNo" width="180" />
        <el-table-column label="原状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.oldStatus" :type="getStatusType(scope.row.oldStatus)">
              {{ getStatusText(scope.row.oldStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="新状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.newStatus)">
              {{ getStatusText(scope.row.newStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" prop="operator" width="120" />
        <el-table-column label="操作IP" prop="operatorIp" width="150" />
        <el-table-column label="操作时间" prop="createTime" width="180" />
      </el-table>

      <pagination
        v-if="total > 0"
        :total="total"
        :page="queryParams.current"
        :limit="queryParams.size"
        @update:page="queryParams.current = $event"
        @update:limit="queryParams.size = $event"
        @pagination="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrderLogPage } from '@/api/marrylink-api'

const queryParams = reactive({
  current: 1,
  size: 10,
  orderNo: ''
})

const pageData = ref([])
const total = ref(0)
const loading = ref(false)

function getStatusType(status) {
  const types = { 1: 'warning', 3: 'info', 4: 'success', 5: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 1: '待确认', 3: '已支付', 4: '已完成', 5: '已取消' }
  return texts[status] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderLogPage(queryParams)
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
  queryParams.orderNo = ''
  handleQuery()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
  background: #ffffff;
}

.search-container {
  margin-bottom: 20px;
}
</style>