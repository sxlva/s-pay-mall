/**
 * 管理端领域类型定义
 *
 * <p>FP1-1：由 api/admin.ts 抽入，类型与 API 函数不再混放；
 * FP1-5：管理端类型统一 AdminVO 后缀（商城端为 VO 后缀）。</p>
 *
 * @author 傅崇睿
 */

/** 用户值对象（后台管理） */
export interface UserAdminVO {
  /** 用户 ID */
  id: number
  /** 用户名 */
  username: string
  /** 邮箱 */
  email: string
  /** 角色名称 */
  role: string
  /** 角色编码：ADMIN-管理员 MEMBER-普通会员 VIP-VIP会员 GUEST-普通用户 */
  roleCode: string
  /** 状态：1-正常 0-禁用 */
  status: number
  /** 创建时间（驼峰字段） */
  createTime: string
  /** 创建时间（下划线字段） */
  create_time: string
  /** 更新时间（驼峰字段） */
  updateTime: string
  /** 更新时间（下划线字段） */
  update_time: string
}

/** 用户查询参数（后台管理） */
export interface UserAdminQueryParams {
  /** 按用户名搜索 */
  username?: string
  /** 按状态过滤：1-正常 0-禁用 */
  status?: number
  /** 按角色编码过滤 */
  roleCode?: string
}

/** 分类值对象（后台管理） */
export interface CategoryAdminVO {
  /** 分类 ID */
  id: number
  /** 分类名称 */
  name: string
  /** 状态：1-启用 0-禁用 */
  status: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 商品值对象（后台管理） */
export interface ProductAdminVO {
  /** 商品 ID */
  id: number
  /** 所属分类 ID */
  categoryId: number
  /** 分类名称 */
  categoryName: string
  /** 商品名称 */
  name: string
  /** 商品描述 */
  description: string
  /** 商品图片访问路径（相对路径；为空时展示本地默认图片） */
  image_url?: string
  /** 商品价格 */
  price: number
  /** 库存数量 */
  stock: number
  /** 状态：1-上架 0-下架 */
  status: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 订单商品项（后台管理） */
export interface OrderItemAdminVO {
  /** 订单项 ID */
  id: number
  /** 商品 ID */
  productId: number
  /** 商品名称 */
  productName: string
  /** 商品单价 */
  price: number
  /** 购买数量 */
  quantity: number
  /** 小计金额 */
  itemAmount: number
}

/** 订单值对象（后台管理） */
export interface OrderAdminVO {
  /** 订单 ID */
  id: number
  /** 订单编号 */
  orderNo: string
  /** 用户 ID */
  userId: number
  /** 订单总金额 */
  totalAmount: number
  /** 收货地址 */
  address: string
  /** 订单状态 */
  status: string
  /** 订单状态描述 */
  statusDesc: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
  /** 商品总数量 */
  totalCount: number
  /** 订单商品列表 */
  items: OrderItemAdminVO[]
}

/** 订单查询参数（后台管理） */
export interface OrderAdminQueryParams {
  /** 按状态过滤 */
  status?: string
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
  /** 分页页码 */
  page?: number
  /** 每页大小 */
  size?: number
}

/** 销售额走势数据（后台管理） */
export interface SalesTrendAdminVO {
  /** 日期 */
  date: string
  /** 销售额 */
  amount: number
  /** 订单数 */
  count: number
}

/** 分类销售占比数据（后台管理） */
export interface CategoryRatioAdminVO {
  /** 分类 ID */
  categoryId: number
  /** 分类名称 */
  categoryName: string
  /** 销售额 */
  amount: number
  /** 占比 */
  ratio: number
}
