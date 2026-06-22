/**
 * API 请求工具：统一附带 token 并处理后端响应格式
 *
 * @author 傅崇睿
 */

const BASE_URL = ''

/** 请求配置选项 */
export interface RequestOptions {
  /** HTTP 方法 */
  method?: string
  /** 请求体数据 */
  data?: Record<string, unknown>
  /** 自定义请求头 */
  headers?: Record<string, string>
}

/** 统一的后端 API 响应格式 */
export interface ApiResponse<T = unknown> {
  /** 状态码：'0000' 表示成功 */
  code: string | number
  /** 错误信息（旧字段） */
  info?: string
  /** 错误信息（新字段） */
  msg?: string
  /** 响应数据 */
  data: T
}

/**
 * 通用 API 请求函数
 * @param path 请求路径
 * @param method HTTP 方法（默认 GET）
 * @param data 请求体数据
 * @returns 解析后的响应数据
 * @throws Error 当后端返回非成功状态码时抛出
 */
export async function request<T = unknown>(
  path: string,
  method: string = 'GET',
  data?: Record<string, unknown>
): Promise<T> {
  const token = localStorage.getItem('token')
  const options: RequestInit = {
    method,
    headers: {
      'Content-Type': 'application/json'
    }
  }
  if (token) {
    options.headers = {
      ...options.headers,
      Authorization: `Bearer ${token}`
    }
  }
  if (data) {
    options.body = JSON.stringify(data)
  }
  const response = await fetch(`${BASE_URL}${path}`, options)
  const result: ApiResponse<T> = await response.json()
  const successCodes = ['0000', '0', 0, 200, '200']
  if (!successCodes.includes(result.code)) {
    throw new Error(result.info || result.msg || '请求失败')
  }
  return result.data
}
