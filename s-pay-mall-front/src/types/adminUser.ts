/**
 * 管理员用户类型 - 向后兼容
 * @deprecated 请使用 src/types/domain/user.ts
 *
 * @author 傅崇睿
 */

// 重新导出 domain 层类型
export type { UserVO, UserQueryParams } from './domain/user'

// 为保持向后兼容，保留原始接口定义（已被 domain/user.ts 替代）
