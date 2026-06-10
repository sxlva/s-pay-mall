package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.dto.StockChangeMsg;

/**
 * 库存变更策略处理器接口
 * 实现策略模式，支持不同类型的库存变更业务逻辑
 *
 * 【DDD 原则】
 * - Domain 层定义业务行为接口
 * - 策略实现类负责具体业务逻辑
 * - Infrastructure 层负责技术实现（如 Redis、DB 操作）
 *
 * 【设计原则】
 * - 每个策略类只处理一种变更类型（单一职责）
 * - 通过 supports() 方法实现策略的自动路由
 * - handle() 方法返回处理后的库存值
 */
public interface StockChangeHandler {

    /**
     * 处理库存变更业务逻辑
     *
     * @param productId 商品ID
     * @param msg       库存变更消息
     * @return 处理后的库存值
     * @throws RuntimeException 处理失败时抛出异常，触发 MQ 重试机制
     */
    long handle(Long productId, StockChangeMsg msg);

    /**
     * 判断当前处理器是否支持该变更类型
     * 用于策略路由，自动匹配对应的 Handler
     *
     * @param changeType 变更类型
     * @return true=支持，false=不支持
     */
    boolean supports(String changeType);

    /**
     * 获取处理器支持的变更类型
     * 用于日志记录和问题排查
     *
     * @return 变更类型
     */
    String getSupportedChangeType();
}
