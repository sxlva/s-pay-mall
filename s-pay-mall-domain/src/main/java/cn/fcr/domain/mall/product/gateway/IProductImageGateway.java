package cn.fcr.domain.mall.product.gateway;

import cn.fcr.domain.mall.product.model.valobj.ProductImageFile;

import java.util.List;
import java.util.Set;

/**
 * 商品图片存储网关
 *
 * <p>领域层定义的存储抽象，屏蔽底层存储介质（本地磁盘 / 对象存储）。
 * 当前实现为本地磁盘存储，未来迁移 OSS 只需替换 Infrastructure 实现。</p>
 *
 * @author 傅崇睿
 */
public interface IProductImageGateway {

    /** 允许存储/清理的图片扩展名（小写，不含点） */
    Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /**
     * 保存商品图片
     *
     * @param data             图片文件字节内容
     * @param originalFilename 原始文件名，仅用于取扩展名
     * @return 图片的访问相对路径（如 /uploads/products/xxx.jpg），由商品 image_url 字段存储
     */
    String saveProductImage(byte[] data, String originalFilename);

    /**
     * 列出商品图片目录下的全部文件（含非图片文件，由调用方决定如何处理）
     *
     * @return 文件名 + 创建时间列表；目录不存在或为空时返回空列表
     */
    List<ProductImageFile> listProductImages();

    /**
     * 按文件名删除商品图片
     *
     * <p>实现侧必须将文件名约束在固定的商品图片目录内，
     * 拒绝包含路径分隔符或 ".." 的非法文件名；文件不存在时静默跳过。</p>
     *
     * @param filename 纯文件名
     */
    void deleteProductImage(String filename);
}
