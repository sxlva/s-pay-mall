package cn.fcr.domain.mall.product.model.valobj;

import java.time.Instant;

/**
 * 商品图片文件描述（本地存储视角）
 *
 * @param filename  文件名（纯文件名，不含目录）
 * @param createdAt 文件创建时间，用于保护期判断
 * @author 傅崇睿
 */
public record ProductImageFile(
        String filename,
        Instant createdAt
) {
}
