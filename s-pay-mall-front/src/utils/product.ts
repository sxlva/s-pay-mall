import type { ProductVO } from '@/types/product'

export function normalizeStock(stock: number | string | null | undefined): number {
  const value = Number(stock)
  return Number.isFinite(value) ? value : 0
}

export function isSoldOut(stock: number | string | null | undefined): boolean {
  return normalizeStock(stock) <= 0
}

export function normalizeProduct(product: ProductVO): ProductVO {
  return {
    ...product,
    stock: normalizeStock(product.stock),
  }
}

export function normalizeProducts(products: ProductVO[]): ProductVO[] {
  return products.map(normalizeProduct)
}
