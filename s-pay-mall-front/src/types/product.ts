export interface ProductVO {
  id: number
  category_id: number
  category_name: string
  name: string
  description: string
  price: number
  stock: number
  status: number
  create_time: string
  update_time: string
}

export interface CategoryVO {
  id: number
  name: string
  status: number
  create_time: string
  update_time: string
}

export interface ProductQueryParams {
  categoryId?: number | null
  keyword?: string
  minPrice?: number
  maxPrice?: number
  status?: number
}