/**
 * 管理员后台-统计数据 API 函数
 *
 * <p>FP0-2：由 api/admin.ts 按业务拆分迁入。</p>
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../../utils/axios'
import type { SalesTrendAdminVO, CategoryRatioAdminVO } from '../../types/domain/admin'

/**
 * 获取销售额走势
 * @returns 走势数据列表
 */
export const getSalesTrend = (): Promise<SalesTrendAdminVO[]> => {
  return adminInstance.get('/admin/statistics/sales-trend')
}

/**
 * 获取分类销售占比
 * @returns 占比数据列表
 */
export const getCategoryRatio = (): Promise<CategoryRatioAdminVO[]> => {
  return adminInstance.get('/admin/statistics/category-ratio')
}
