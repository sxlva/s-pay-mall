package cn.fcr.trigger.job;

import cn.fcr.application.ProductImageCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 商品图片孤儿文件清理Job
 *
 * <p>每日凌晨执行一次，清理 uploads/products/ 下未被任何商品引用、
 * 且创建时间超过保护期的图片文件。不参与商品核心业务流程，
 * 可通过 app.config.image-cleanup-enabled 一键关停。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class ImageCleanupJob {

    /** 商品图片清理应用服务 */
    @Resource
    private ProductImageCleanupService productImageCleanupService;

    /** 清理任务开关 */
    @Value("${app.config.image-cleanup-enabled:true}")
    private boolean enabled;

    /**
     * 定时清理孤儿商品图片
     *
     * <p>默认每日 04:00 执行，cron 可通过 app.config.image-cleanup-cron 调整。</p>
     */
    @Scheduled(cron = "${app.config.image-cleanup-cron:0 0 4 * * ?}")
    public void exec() {
        if (!enabled) {
            log.info("商品图片清理任务已关闭，跳过执行");
            return;
        }
        try {
            ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();
            log.info("商品图片清理完成：扫描 {} 个文件，当前引用 {} 个，删除 {} 个，保护期跳过 {} 个，非图片跳过 {} 个，失败 {} 个，耗时 {}ms",
                    summary.scanned(), summary.referenced(), summary.deleted(),
                    summary.protectedSkipped(), summary.nonImageSkipped(), summary.failed(),
                    summary.elapsedMillis());
        } catch (Exception e) {
            log.error("商品图片清理任务执行失败", e);
        }
    }
}
