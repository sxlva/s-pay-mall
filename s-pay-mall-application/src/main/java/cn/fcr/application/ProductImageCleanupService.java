package cn.fcr.application;

import cn.fcr.domain.mall.product.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.product.gateway.IProductImageGateway;
import cn.fcr.domain.mall.product.model.valobj.ProductImageFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 商品图片清理应用服务
 *
 * <p>定时清理孤儿商品图片：比对数据库引用关系与本地目录文件，
 * 删除"未被引用且创建时间超过保护期"的图片文件。
 * 不参与商品核心业务事务，单文件失败不影响其他文件，任务幂等。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Service
public class ProductImageCleanupService {

    /** 商品仓储 */
    @Resource
    private IProductRepository productRepository;

    /** 商品图片存储网关 */
    @Resource
    private IProductImageGateway productImageGateway;

    /** 保护期（小时）：创建时间距今不足该时长的未引用文件不清理，避免误删刚上传未完成保存的图片 */
    @Value("${app.config.image-cleanup-protection-hours:24}")
    private int protectionHours;

    /**
     * 执行一次孤儿图片清理
     *
     * @return 清理结果统计
     */
    public CleanupSummary cleanupOrphanProductImages() {
        long startMillis = System.currentTimeMillis();

        Set<String> referencedFilenames = extractReferencedFilenames(productRepository.selectAllImageUrls());
        List<ProductImageFile> files = productImageGateway.listProductImages();
        Instant cutoff = Instant.now().minus(protectionHours, ChronoUnit.HOURS);

        int deleted = 0;
        int protectedSkipped = 0;
        int nonImageSkipped = 0;
        int failed = 0;

        for (ProductImageFile file : files) {
            String filename = file.filename();
            if (isNonImage(filename)) {
                nonImageSkipped++;
                continue;
            }
            if (referencedFilenames.contains(filename)) {
                continue;
            }
            if (file.createdAt().isAfter(cutoff)) {
                protectedSkipped++;
                continue;
            }
            try {
                productImageGateway.deleteProductImage(filename);
                deleted++;
                log.info("清理孤儿商品图片: {}", filename);
            } catch (Exception e) {
                failed++;
                log.warn("清理孤儿商品图片失败: {}", filename, e);
            }
        }

        long elapsed = System.currentTimeMillis() - startMillis;
        return new CleanupSummary(files.size(), referencedFilenames.size(), deleted,
                protectedSkipped, nonImageSkipped, failed, elapsed);
    }

    /**
     * 从数据库引用的图片路径中提取纯文件名集合
     *
     * <p>只取最后一个 '/' 之后的部分，并拒绝含 ".." 或路径分隔符的异常路径，
     * 确保 ../../xxx 之类的路径不可能参与后续文件删除比对。</p>
     *
     * @param imageUrls 数据库中的 image_url 集合
     * @return 安全的纯文件名集合
     */
    private Set<String> extractReferencedFilenames(Set<String> imageUrls) {
        Set<String> filenames = new HashSet<>();
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            String filename = url.substring(url.lastIndexOf('/') + 1).trim();
            if (filename.isEmpty() || filename.contains("..")
                    || filename.contains("/") || filename.contains("\\")) {
                log.warn("忽略异常图片路径，不参与清理比对: {}", url);
                continue;
            }
            filenames.add(filename);
        }
        return filenames;
    }

    /**
     * 是否非图片文件（不在扩展名白名单内）
     *
     * @param filename 文件名
     * @return true=非图片文件
     */
    private boolean isNonImage(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return true;
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return !IProductImageGateway.IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * 清理结果统计
     *
     * @param scanned          扫描到的文件总数
     * @param referenced       当前被商品引用的图片数
     * @param deleted          实际删除数
     * @param protectedSkipped 保护期跳过的未引用文件数
     * @param nonImageSkipped  非图片文件跳过数
     * @param failed           删除失败数
     * @param elapsedMillis    耗时
     */
    public record CleanupSummary(
            int scanned,
            int referenced,
            int deleted,
            int protectedSkipped,
            int nonImageSkipped,
            int failed,
            long elapsedMillis
    ) {
    }
}
