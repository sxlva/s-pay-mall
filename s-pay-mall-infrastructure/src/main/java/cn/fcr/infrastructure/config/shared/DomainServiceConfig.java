package cn.fcr.infrastructure.config.shared;

import cn.fcr.domain.auth.gateway.IWeChatGateway;
import cn.fcr.domain.auth.gateway.IWechatLoginGateway;
import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.domain.auth.repository.IWeChatTokenRepository;
import cn.fcr.domain.auth.service.WeixinBindService;
import cn.fcr.domain.auth.service.WeixinLoginService;
import cn.fcr.domain.mall.adapter.gateway.IDistributedLockService;
import cn.fcr.domain.mall.adapter.repository.ICartRepository;
import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.adapter.repository.IStatisticsRepository;
import cn.fcr.domain.mall.adapter.repository.IUserRepository;
import cn.fcr.domain.mall.gateway.IAuthTokenGateway;
import cn.fcr.domain.mall.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.gateway.IOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IPayOrderGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.gateway.IUserBindingGateway;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.domain.mall.service.IMallStatisticsService;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.domain.mall.service.IOrderStateMachineService;
import cn.fcr.domain.mall.service.handler.AdminUpdateHandler;
import cn.fcr.domain.mall.service.handler.DeductHandler;
import cn.fcr.domain.mall.service.handler.RestoreHandler;
import cn.fcr.domain.mall.service.impl.MallCartServiceImpl;
import cn.fcr.domain.mall.service.impl.MallOrderServiceImpl;
import cn.fcr.domain.mall.service.impl.MallProductServiceImpl;
import cn.fcr.domain.mall.service.impl.MallStatisticsServiceImpl;
import cn.fcr.domain.mall.service.impl.MallUserServiceImpl;
import cn.fcr.domain.mall.service.impl.OrderStateMachineServiceImpl;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务手动装配配置
 * <p>
 * 【DDD 重构】由于 Domain 层已移除所有 Spring 注解（@Service、@Component、@Resource），
 * 此配置负责将纯 POJO 的领域服务注册为 Spring Bean，并完成依赖注入。
 * <p>
 * 包含：
 * - 微信登录服务：WeixinLoginService
 * - 微信绑定服务：WeixinBindService
 * - 商城购物车服务：MallCartServiceImpl
 * - 商城商品服务：MallProductServiceImpl
 * - 商城用户服务：MallUserServiceImpl
 * - 商城统计服务：MallStatisticsServiceImpl
 * - 订单领域服务：OrderService
 * - 订单状态机服务：OrderStateMachineServiceImpl
 * - 商城订单服务：MallOrderServiceImpl
 * - 库存变更策略处理器：DeductHandler、AdminUpdateHandler、RestoreHandler
 */
@Configuration
public class DomainServiceConfig {

    // ==================== 微信登录服务 ====================

    @Bean
    public WeixinLoginService weixinLoginService(IWeChatGateway weChatGateway,
                                                 IWechatLoginGateway wechatLoginGateway,
                                                 ITokenProvider tokenProvider) {
        return new WeixinLoginService(weChatGateway, wechatLoginGateway, tokenProvider);
    }

    // ==================== 微信绑定服务 ====================

    @Bean
    public WeixinBindService weixinBindService(IWeChatTokenRepository weChatTokenRepository) {
        return new WeixinBindService(weChatTokenRepository);
    }

    // ==================== 商城购物车服务 ====================

    @Bean
    public IMallCartService mallCartService(ICartRepository cartRepository,
                                            IProductRepository productRepository,
                                            IDistributedLockService distributedLockService) {
        return new MallCartServiceImpl(cartRepository, productRepository, distributedLockService);
    }

    // ==================== 商城商品服务 ====================

    @Bean
    public IMallProductService mallProductService(IProductRepository productRepository) {
        return new MallProductServiceImpl(productRepository);
    }

    // ==================== 商城用户服务 ====================

    @Bean
    public IMallUserService mallUserService(IUserRepository userRepository,
                                            IAuthTokenGateway authTokenGateway,
                                            IOrderQueryGateway orderQueryGateway,
                                            IUserBindingGateway userBindingGateway) {
        return new MallUserServiceImpl(userRepository, authTokenGateway, orderQueryGateway, userBindingGateway);
    }

    // ==================== 商城统计服务 ====================

    @Bean
    public IMallStatisticsService mallStatisticsService(IStatisticsRepository statisticsRepository) {
        return new MallStatisticsServiceImpl(statisticsRepository);
    }

    // ==================== 订单领域服务 ====================

    @Bean
    public OrderService orderService(IOrderRepository orderRepository,
                                     IProductGateway productGateway,
                                     IPaymentGateway paymentGateway) {
        return new OrderService(orderRepository, productGateway, paymentGateway);
    }

    // ==================== 订单状态机服务 ====================

    @Bean
    public OrderStateMachineServiceImpl orderStateMachineService(IMallOrderQueryGateway mallOrderQueryGateway,
                                                                 IPayOrderGateway payOrderGateway,
                                                                 IStockGateway stockGateway) {
        return new OrderStateMachineServiceImpl(mallOrderQueryGateway, payOrderGateway, stockGateway);
    }

    // ==================== 商城订单服务 ====================

    @Bean
    public MallOrderServiceImpl mallOrderService(IMallOrderQueryGateway mallOrderQueryGateway,
                                                 IOrderPaymentGateway orderPaymentGateway,
                                                 IOrderStateMachineService orderStateMachineService,
                                                 IStockGateway stockGateway) {
        return new MallOrderServiceImpl(mallOrderQueryGateway,
                orderPaymentGateway, orderStateMachineService, stockGateway);
    }

    // ==================== 库存变更策略处理器 ====================

    @Bean
    public DeductHandler deductHandler(IIdempotentGateway idempotentGateway,
                                       IStockGateway stockGateway) {
        return new DeductHandler(idempotentGateway, stockGateway);
    }

    @Bean
    public AdminUpdateHandler adminUpdateHandler(IIdempotentGateway idempotentGateway,
                                                 IStockGateway stockGateway) {
        return new AdminUpdateHandler(idempotentGateway, stockGateway);
    }

    @Bean
    public RestoreHandler restoreHandler(IIdempotentGateway idempotentGateway,
                                         IStockGateway stockGateway) {
        return new RestoreHandler(idempotentGateway, stockGateway);
    }
}
