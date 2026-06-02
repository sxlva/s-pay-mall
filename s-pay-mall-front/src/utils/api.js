// API请求工具：统一附带token并处理后端响应格式
const BASE_URL = ''

export async function request(path, method = 'GET', data) {
  const token = localStorage.getItem('token')
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json'
    }
  }
  if (token) {
    options.headers.Authorization = `Bearer ${token}`
  }
  if (data) {
    options.body = JSON.stringify(data)
  }
  const response = await fetch(`${BASE_URL}${path}`, options)
  const result = await response.json()
  if (result.code !== '0000') {
    throw new Error(result.info || '请求失败')
  }
  return result.data
}
