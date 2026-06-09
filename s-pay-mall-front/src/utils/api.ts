// API 请求工具：统一附带 token 并处理后端响应格式

const BASE_URL = ''

export interface RequestOptions {
  method?: string
  data?: Record<string, unknown>
  headers?: Record<string, string>
}

export interface ApiResponse<T = unknown> {
  code: string | number
  info?: string
  msg?: string
  data: T
}

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
