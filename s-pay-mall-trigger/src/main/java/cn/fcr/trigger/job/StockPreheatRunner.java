package cn.fcr.trigger.job;

import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.gateway.IStockGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 库存预热启动类
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
@Order(2)
public class StockPreheatRunner implements ApplicationRunner {

    /** 商品仓储 */
    @Resource
    private IProductRepository productRepository;

    /** 库存网关 */
    @Resource
    private IStockGateway stockGateway;

    /**
     * 应用启动后执行全量库存预热
     *
     * <p>查询所有上架商品（status=1），批量同步库存到 Redis。
     * 预热失败不影响应用启动，仅记录日志。
     * 执行顺序 Order=2（在默认管理员初始化之后）。</p>
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("【库存预热】开始执行全量库存预热...");

        try {
            // 1. 查询所有上架商品ID
            List<Long> activeProductIds = productRepository.queryAllActiveProductIds();

            if (activeProductIds == null || activeProductIds.isEmpty()) {
                log.info("【库存预热】无上架商品，跳过预热");
                return;
            }

            log.info("【库存预热】查询到 {} 个上架商品，开始批量同步库存", activeProductIds.size());

            // 2. 批量同步库存到 Redis
            int successCount = stockGateway.batchSyncStock(activeProductIds);

            // 3. 记录预热结果
            log.info("【库存预热完成】总数={}, 成功={}, 失败={}",
                    activeProductIds.size(), successCount, activeProductIds.size() - successCount);

        } catch (Exception e) {
            // 预热失败不影响应用启动，仅记录日志
            log.error("【库存预热异常】预热失败，应用正常启动，Redis库存将在首次访问时懒加载", e);
        }
    }
}
