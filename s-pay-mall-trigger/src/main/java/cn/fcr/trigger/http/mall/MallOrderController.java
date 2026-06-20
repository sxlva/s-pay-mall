package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.CartAddRequestDTO;
import cn.fcr.api.dto.CartItemRespDTO;
import cn.fcr.api.dto.OrderCreateRequestDTO;
import cn.fcr.api.dto.OrderCreateRespDTO;
import cn.fcr.api.dto.OrderListRespDTO;
import cn.fcr.api.dto.StockCheckRespDTO;
import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
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
 * 订单控制器
 * 处理购物车和订单相关接口
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class MallOrderController extends BaseController {

    @Resource
    private IMallCartService mallCartService;

    @Resource
    private IMallOrderService mallOrderService;

    @PostMapping("/cart")
    public Response<Integer> addCart(@RequestBody CartAddRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        log.info("添加购物车: userId={}, productId={}, quantity={}", userId, request.getProductId(), quantity);
        int result = mallCartService.addCart(userId, request.getProductId(), quantity);
        return success(result);
    }

    @GetMapping("/cart")
    public Response<List<CartItemRespDTO>> listCart(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        List<CartItemVO> cartItems = mallCartService.listCart(userId);
        List<CartItemRespDTO> result = cartItems.stream()
                .map(CartItemConverter.INSTANCE::toRespDTO)
                .collect(Collectors.toList());
        return success(result);
    }

    @PutMapping("/cart/quantity")
    public Response<Integer> updateCartQuantity(@RequestBody CartAddRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            return fail("数量不能小于1");
        }
        log.info("更新购物车数量: userId={}, productId={}, quantity={}", userId, request.getProductId(), quantity);
        int result = mallCartService.updateQuantity(userId, request.getProductId(), quantity);
        return success(result);
    }

    @DeleteMapping("/cart/delete")
    public Response<Integer> deleteCartItem(@RequestParam("itemId") Long itemId, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("[购物车删除] 接收到删除请求，用户ID: {}, 目标条目ID: {}", userId, itemId);
        int result = mallCartService.deleteCartItem(userId, itemId);
        log.info("[购物车删除] 删除完成，影响行数: {}", result);
        return success(result);
    }

    @PostMapping("/orders")
    public Response<OrderCreateRespDTO> createOrder(@RequestBody OrderCreateRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("创建订单: userId={}, address={}", userId, request.getAddress());
        OrderCreateVO orderVO = mallOrderService.createOrder(userId, request.getAddress());

        OrderCreateRespDTO result = OrderConverter.INSTANCE.toCreateResp(orderVO);
        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.setHtml(orderVO.getPayUrl());
        }

        return success(result);
    }

    @GetMapping("/orders")
    public Response<List<OrderListRespDTO>> listOrders(HttpServletRequest httpRequest,
                                                       @RequestParam(value = "status", required = false) String status,
                                                       @RequestParam(value = "startTime", required = false) String startTime,
                                                       @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = currentUserId(httpRequest);
        List<OrderVO> orders = mallOrderService.listOrders(userId, status, startTime, endTime);
        List<OrderListRespDTO> result = orders.stream()
                .map(OrderListConverter.INSTANCE::toRespDTO)
                .collect(Collectors.toList());
        return success(result);
    }

    @GetMapping("/orders/{orderNo}/continue-pay")
    public Response<OrderCreateRespDTO> continuePay(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("继续支付订单: userId={}, orderNo={}", userId, orderNo);

        OrderCreateVO orderVO = mallOrderService.continuePay(orderNo);

        OrderCreateRespDTO result = OrderConverter.INSTANCE.toCreateResp(orderVO);
        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.setHtml(orderVO.getPayUrl());
        }

        return success(result);
    }

    @GetMapping("/orders/{orderNo}/check-stock")
    public Response<StockCheckRespDTO> checkStock(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("检查订单库存: userId={}, orderNo={}", userId, orderNo);

        boolean stockOk = mallOrderService.checkOrderStock(orderNo);

        StockCheckRespDTO result = StockCheckRespDTO.builder()
                .success(stockOk)
                .message(stockOk ? null : "库存不足，无法继续支付")
                .build();

        return success(result);
    }
}
