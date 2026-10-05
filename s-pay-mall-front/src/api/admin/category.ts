/**
 * 管理员后台-分类管理 API 函数
 *
 * <p>FP0-2：由 api/admin.ts 按业务拆分迁入。</p>
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../../utils/axios'
import type { CategoryAdminVO } from '../../types/domain/admin'
import type { SaveCategoryParams } from '../../types/api/admin'

/**
 * 获取分类列表
 * @returns 分类列表
 */
export const getAdminCategories = (): Promise<CategoryAdminVO[]> => {
  return adminInstance.get('/admin/categories')
}

/**
 * 创建或更新分类
 * @param data 分类数据
 * @returns 保存后的分类数据
 */
export const saveAdminCategory = (data: SaveCategoryParams): Promise<CategoryAdminVO> => {
  return adminInstance.post('/admin/categories', data)
}

/**
 * 删除分类
 * @param id 分类 ID
 * @returns void
 */
export const deleteAdminCategory = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/categories/${id}`)
}
