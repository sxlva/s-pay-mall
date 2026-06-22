package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.CartAddRequestDTO;
import cn.fcr.api.dto.CartItemRespDTO;
import cn.fcr.api.dto.OrderCreateRequestDTO;
import cn.fcr.api.dto.OrderCreateRespDTO;
import cn.fcr.api.dto.OrderListRespDTO;
import cn.fcr.api.dto.StockCheckRespDTO;
import cn.fcr.api.response.Response;
import cn.fcr.trigger.application.OrderApplicationService;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.trigger.http.BaseController;
import cn.fcr.trigger.http.converter.CartItemConverter;
import cn.fcr.trigger.http.converter.OrderConverter;
import cn.fcr.trigger.http.converter.OrderListConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单与购物车Controller
 *
 * <p>【DDD 触发层】处理购物车和订单相关接口，包括购物车增删改查、
 * 订单创建、订单列表、继续支付、库存检查。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class MallOrderController extends BaseController {

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /**
     * 添加商品到购物车
     *
     * @param request     购物车添加请求，包含productId和可选quantity
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 影响行数
     */
    @PostMapping("/cart")
    public Response<Integer> addCart(@RequestBody CartAddRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        log.info("添加购物车: userId={}, productId={}, quantity={}", userId, request.getProductId(), quantity);
        int result = orderApplicationService.addCart(userId, request.getProductId(), quantity);
        return success(result);
    }

    /**
     * 查询购物车列表
     *
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 购物车商品列表
     */
    @GetMapping("/cart")
    public Response<List<CartItemRespDTO>> listCart(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        List<CartItemVO> cartItems = orderApplicationService.listCart(userId);
        List<CartItemRespDTO> result = cartItems.stream()
                .map(CartItemConverter.INSTANCE::toRespDTO)
                .collect(Collectors.toList());
        return success(result);
    }

    /**
     * 更新购物车商品数量
     *
     * @param request     购物车更新请求
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 影响行数
     */
    @PutMapping("/cart/quantity")
    public Response<Integer> updateCartQuantity(@RequestBody CartAddRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            return fail("数量不能小于1");
        }
        log.info("更新购物车数量: userId={}, productId={}, quantity={}", userId, request.getProductId(), quantity);
        int result = orderApplicationService.updateCartQuantity(userId, request.getProductId(), quantity);
        return success(result);
    }

    /**
     * 删除购物车商品
     *
     * @param itemId      购物车条目ID
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 影响行数
     */
    @DeleteMapping("/cart/delete")
    public Response<Integer> deleteCartItem(@RequestParam("itemId") Long itemId, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("[购物车删除] 接收到删除请求，用户ID: {}, 目标条目ID: {}", userId, itemId);
        int result = orderApplicationService.deleteCartItem(userId, itemId);
        log.info("[购物车删除] 删除完成，影响行数: {}", result);
        return success(result);
    }

    /**
     * 创建订单
     *
     * <p>从购物车结算生成订单，执行库存预扣、支付单生成等操作。
     * 创建成功后返回订单号和支付URL（html字段）。</p>
     *
     * @param request     订单创建请求，包含收货地址
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 订单创建结果，含订单号和支付URL
     */
    @PostMapping("/orders")
    public Response<OrderCreateRespDTO> createOrder(@RequestBody OrderCreateRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("创建订单: userId={}, address={}", userId, request.getAddress());
        OrderCreateVO orderVO = orderApplicationService.createOrder(userId, request.getAddress());

        OrderCreateRespDTO result = OrderConverter.INSTANCE.toCreateResp(orderVO);
        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.setHtml(orderVO.getPayUrl());
        }

        return success(result);
    }

    /**
     * 查询用户订单列表
     *
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @param status      订单状态（可选）
     * @param startTime   开始时间（可选）
     * @param endTime     结束时间（可选）
     * @return 订单列表
     */
    @GetMapping("/orders")
    public Response<List<OrderListRespDTO>> listOrders(HttpServletRequest httpRequest,
                                                       @RequestParam(value = "status", required = false) String status,
                                                       @RequestParam(value = "startTime", required = false) String startTime,
                                                       @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = currentUserId(httpRequest);
        List<OrderVO> orders = orderApplicationService.listOrders(userId, status, startTime, endTime);
        List<OrderListRespDTO> result = orders.stream()
                .map(OrderListConverter.INSTANCE::toRespDTO)
                .collect(Collectors.toList());
        return success(result);
    }

    /**
     * 继续支付未完成订单
     *
     * @param orderNo     订单号
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 订单支付信息，含最新支付URL
     */
    @GetMapping("/orders/{orderNo}/continue-pay")
    public Response<OrderCreateRespDTO> continuePay(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("继续支付订单: userId={}, orderNo={}", userId, orderNo);

        OrderCreateVO orderVO = orderApplicationService.continuePay(orderNo);

        OrderCreateRespDTO result = OrderConverter.INSTANCE.toCreateResp(orderVO);
        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.setHtml(orderVO.getPayUrl());
        }

        return success(result);
    }

    /**
     * 检查订单库存是否充足
     *
     * @param orderNo     订单号
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 库存检查结果
     */
    @GetMapping("/orders/{orderNo}/check-stock")
    public Response<StockCheckRespDTO> checkStock(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("检查订单库存: userId={}, orderNo={}", userId, orderNo);

        boolean stockOk = orderApplicationService.checkStock(orderNo);

        StockCheckRespDTO result = StockCheckRespDTO.builder()
                .success(stockOk)
                .message(stockOk ? null : "库存不足，无法继续支付")
                .build();

        return success(result);
    }
}
