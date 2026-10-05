/**
 * 管理员后台-商品管理 API 函数
 *
 * <p>FP0-2：由 api/admin.ts 按业务拆分迁入。</p>
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../../utils/axios'
import type { ProductAdminVO } from '../../types/domain/admin'
import type { SaveProductParams } from '../../types/api/admin'

/**
 * 获取商品列表
 * @param params 查询参数
 * @returns 商品列表
 */
export const getAdminProducts = (params: Record<string, unknown> = {}): Promise<ProductAdminVO[]> => {
  return adminInstance.get('/admin/products', { params })
}

/**
 * 创建或更新商品
 * @param data 商品数据
 * @returns 保存后的商品数据
 */
export const saveAdminProduct = (data: SaveProductParams): Promise<ProductAdminVO> => {
  return adminInstance.post('/admin/products', data)
}

/**
 * 删除商品
 * @param id 商品 ID
 * @returns void
 */
export const deleteAdminProduct = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/products/${id}`)
}

/**
 * 上传商品图片
 * @param file 图片文件（jpg/jpeg/png/webp/gif，最大 5MB）
 * @returns 图片访问相对路径（如 /uploads/products/xxx.jpg）
 */
export const uploadAdminProductImage = (file: File): Promise<{ url: string }> => {
  const formData = new FormData()
  formData.append('file', file)
  return adminInstance.post('/admin/files/product-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
