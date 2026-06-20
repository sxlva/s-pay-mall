package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.CartAddRequestDTO;
import cn.fcr.api.dto.OrderCreateRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Response<List<Map<String, Object>>> listCart(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        List<CartItemVO> cartItems = mallCartService.listCart(userId);
        List<Map<String, Object>> result = cartItems.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("productId", item.getProductId());
            map.put("productName", item.getProductName());
            map.put("price", item.getProductPrice());
            map.put("quantity", item.getQuantity());
            map.put("selected", item.getSelected());
            map.put("itemAmount", item.getItemAmount());
            map.put("stock", item.getStock());
            return map;
        }).collect(Collectors.toList());
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
    public Response<Map<String, Object>> createOrder(@RequestBody OrderCreateRequestDTO request, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("创建订单: userId={}, address={}", userId, request.getAddress());
        OrderCreateVO orderVO = mallOrderService.createOrder(userId, request.getAddress());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderVO.getOrderNo());
        result.put("payUrl", orderVO.getPayUrl());
        result.put("totalAmount", orderVO.getTotalAmount());

        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.put("_html", orderVO.getPayUrl());
        }

        return success(result);
    }

    @GetMapping("/orders")
    public Response<List<Map<String, Object>>> listOrders(HttpServletRequest httpRequest,
                                                          @RequestParam(value = "status", required = false) String status,
                                                          @RequestParam(value = "startTime", required = false) String startTime,
                                                          @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = currentUserId(httpRequest);
        List<OrderVO> orders = mallOrderService.listOrders(userId, status, startTime, endTime);
        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("status", order.getStatus());
            map.put("statusDesc", order.getStatusDesc());
            map.put("totalAmount", order.getTotalAmount());
            map.put("createTime", order.getCreateTime());
            map.put("address", order.getAddress());
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @GetMapping("/orders/{orderNo}/continue-pay")
    public Response<Map<String, Object>> continuePay(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("继续支付订单: userId={}, orderNo={}", userId, orderNo);

        OrderCreateVO orderVO = mallOrderService.continuePay(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderVO.getOrderNo());
        result.put("payUrl", orderVO.getPayUrl());
        result.put("totalAmount", orderVO.getTotalAmount());

        if (orderVO.getPayUrl() != null && !orderVO.getPayUrl().isEmpty()) {
            result.put("_html", orderVO.getPayUrl());
        }

        return success(result);
    }

    @GetMapping("/orders/{orderNo}/check-stock")
    public Response<Map<String, Object>> checkStock(@PathVariable("orderNo") String orderNo, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("检查订单库存: userId={}, orderNo={}", userId, orderNo);

        boolean stockOk = mallOrderService.checkOrderStock(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", stockOk);
        
        if (!stockOk) {
            result.put("message", "库存不足，无法继续支付");
        }

        return success(result);
    }
}
