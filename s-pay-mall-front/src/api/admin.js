import { adminInstance } from '../utils/axios'

// ==================== 用户管理 ====================

export const getAdminUsers = (params = {}) => {
  return adminInstance.get('/admin/users', { params })
}

export const saveAdminUser = (data) => {
  return adminInstance.post('/admin/users', data)
}

export const updateAdminUserStatus = (id, status) => {
  return adminInstance.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export const deleteAdminUser = (id) => {
  return adminInstance.delete(`/admin/users/${id}`)
}

// ==================== 分类管理 ====================

export const getAdminCategories = () => {
  return adminInstance.get('/admin/categories')
}

export const saveAdminCategory = (data) => {
  return adminInstance.post('/admin/categories', data)
}

export const deleteAdminCategory = (id) => {
  return adminInstance.delete(`/admin/categories/${id}`)
}

// ==================== 商品管理 ====================

export const getAdminProducts = (params = {}) => {
  return adminInstance.get('/admin/products', { params })
}

export const saveAdminProduct = (data) => {
  return adminInstance.post('/admin/products', data)
}

export const deleteAdminProduct = (id) => {
  return adminInstance.delete(`/admin/products/${id}`)
}

// ==================== 订单管理 ====================

export const getAdminOrders = (params = {}) => {
  return adminInstance.get('/admin/orders', { params })
}

export const deliverOrder = (id) => {
  return adminInstance.put(`/admin/orders/${id}/deliver`)
}

export const cancelOrder = (id) => {
  return adminInstance.put(`/admin/orders/${id}/cancel`)
}

export const updateAdminOrderStatus = (id, status) => {
  return adminInstance.put(`/admin/orders/${id}/status`, { status })
}

export const deleteAdminOrder = (id) => {
  return adminInstance.delete(`/admin/orders/${id}`)
}

// ==================== 统计数据 ====================

export const getSalesTrend = () => {
  return adminInstance.get('/admin/statistics/sales-trend')
}

export const getCategoryRatio = () => {
  return adminInstance.get('/admin/statistics/category-ratio')
}