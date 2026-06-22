/**
 * 商品工具函数：库存规范化与售罄判断
 *
 * @author 傅崇睿
 */

import type { ProductVO } from '@/types/product'

/**
 * 规范化库存值为数字
 * @param stock 原始库存值（可能为 null/undefined/string）
 * @returns 规范化后的数字，无效时返回 0
 */
export function normalizeStock(stock: number | string | null | undefined): number {
  const value = Number(stock)
  return Number.isFinite(value) ? value : 0
}

/**
 * 判断商品是否已售罄
 * @param stock 库存值
 * @returns true-已售罄 false-有库存
 */
export function isSoldOut(stock: number | string | null | undefined): boolean {
  return normalizeStock(stock) <= 0
}

/**
 * 规范化单个商品数据
 * @param product 原始商品数据
 * @returns 规范化后的商品数据
 */
export function normalizeProduct(product: ProductVO): ProductVO {
  return {
    ...product,
    stock: normalizeStock(product.stock),
  }
}

/**
 * 规范化商品列表数据
 * @param products 原始商品列表
 * @returns 规范化后的商品列表
 */
export function normalizeProducts(products: ProductVO[]): ProductVO[] {
  return products.map(normalizeProduct)
}
