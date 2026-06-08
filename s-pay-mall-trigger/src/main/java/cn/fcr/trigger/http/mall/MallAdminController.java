package cn.fcr.trigger.http.mall;

import cn.fcr.api.IAdminService;
import cn.fcr.api.dto.CategorySaveRequest;
import cn.fcr.api.dto.ProductSaveRequest;
import cn.fcr.api.dto.UserSaveRequest;
import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.entity.UserEntity;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.domain.mall.service.IMallStatisticsService;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}/admin")
public class MallAdminController extends BaseController implements IAdminService {

    @Resource
    private IMallUserService mallUserService;

    @Resource
    private IMallProductService mallProductService;

    @Resource
    private IMallOrderService mallOrderService;

    @Resource
    private IMallStatisticsService mallStatisticsService;

    @Override
    @GetMapping("/users")
    public Response<List<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String roleCode) {
        List<UserEntity> users = mallUserService.listUsers(username, status, roleCode);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("status", u.getStatus());
            map.put("roleCode", u.getRoleCode());
            map.put("createTime", u.getCreateTime());
            map.put("updateTime", u.getUpdateTime());
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @Override
    @PostMapping("/users")
    public Response<Integer> saveUser(@RequestBody UserSaveRequest request) {
        log.info("保存用户: username={}", request.getUsername());
        UserEntity user = UserEntity.builder()
                .id(request.getId())
                .username(request.getUsername())
                .password(request.getPassword())
                .status(request.getStatus())
                .build();
        int result = mallUserService.saveUser(user);
        return success(result);
    }

    @Override
    @PutMapping("/users/{userId}/status")
    public Response<Integer> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status) {
        log.info("更新用户状态: id={}, status={}", userId, status);
        int result = mallUserService.updateUserStatus(userId, status);
        return success(result);
    }

    @Override
    @DeleteMapping("/users/{userId}")
    public Response<Integer> deleteUser(@PathVariable Long userId) {
        log.info("删除用户: id={}", userId);
        int result = mallUserService.deleteUser(userId);
        return success(result);
    }

    @Override
    @GetMapping("/categories")
    public Response<List<Map<String, Object>>> listCategories() {
        List<CategoryVO> categories = mallProductService.listCategory();
        List<Map<String, Object>> result = categories.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("status", c.getStatus());
            map.put("create_time", c.getCreateTime() != null ? c.getCreateTime().toString() : "");
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @Override
    @PostMapping("/categories")
    public Response<Integer> saveCategory(@RequestBody CategorySaveRequest request) {
        log.info("保存分类: name={}", request.getName());
        int result = mallProductService.saveCategory(toMap(request));
        return success(result);
    }

    @Override
    @DeleteMapping("/categories/{categoryId}")
    public Response<Integer> deleteCategory(@PathVariable Long categoryId) {
        log.info("删除分类: id={}", categoryId);
        int result = mallProductService.deleteCategory(categoryId);
        return success(result);
    }

    @Override
    @GetMapping("/products")
    public Response<List<Map<String, Object>>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer status) {
        List<ProductVO> products = mallProductService.listProducts(categoryId, null, keyword, minPrice, maxPrice, status);
        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("category_id", p.getCategoryId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("price", p.getPrice());
            map.put("stock", p.getStock());
            map.put("category", p.getCategory());
            map.put("category_name", p.getCategoryName());
            map.put("status", p.getStatus());
            map.put("create_time", p.getCreateTime() != null ? p.getCreateTime().toString() : "");
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @Override
    @PostMapping("/products")
    public Response<Integer> saveProduct(@RequestBody ProductSaveRequest request) {
        log.info("保存商品: name={}", request.getName());
        // 将 DTO 转换为领域命令
        ProductSaveCommand command = ProductSaveCommand.builder()
                .id(request.getId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .status(request.getStatus())
                .build();
        int result = mallProductService.saveProduct(command);
        return success(result);
    }

    @Override
    @DeleteMapping("/products/{productId}")
    public Response<Integer> deleteProduct(@PathVariable Long productId) {
        log.info("删除商品: id={}", productId);
        int result = mallProductService.deleteProduct(productId);
        return success(result);
    }

    @Override
    @GetMapping("/orders")
    public Response<List<Map<String, Object>>> listOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        // 通过领域层正确调用订单仓储，确保数据完整性
        List<OrderVO> orders = mallOrderService.listOrders(userId, status, startTime, endTime);
        
        // 防腐层字段映射：严格对齐前端表格绑定的 prop
        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("order_no", o.getOrderNo());      // 前端期望下划线命名
            map.put("user_id", o.getUserId());         // 前端期望下划线命名
            map.put("status", o.getStatus());
            map.put("total_amount", o.getTotalAmount()); // 前端期望下划线命名
            map.put("address", o.getAddress() != null ? o.getAddress() : ""); // 兜底空字符串
            map.put("created_at", o.getCreateTime() != null ? o.getCreateTime().toString() : "");
            return map;
        }).collect(Collectors.toList());
        
        log.info("【订单查询】管理后台查询订单数量: {}", result.size());
        return success(result);
    }

    @Override
    @PutMapping("/orders/{orderId}/deliver")
    public Response<Integer> deliverOrder(@PathVariable Long orderId) {
        log.info("一键发货: orderId={}", orderId);
        int result = mallOrderService.deliverOrder(orderId);
        return success(result);
    }

    @Override
    @PutMapping("/orders/{orderId}/cancel")
    public Response<Integer> cancelOrder(@PathVariable Long orderId) {
        log.info("取消订单: orderId={}", orderId);
        int result = mallOrderService.cancelOrder(orderId);
        return success(result);
    }

    @Override
    @DeleteMapping("/orders/{orderId}")
    public Response<Integer> deleteOrder(@PathVariable Long orderId) {
        log.info("删除订单: id={}", orderId);
        int result = mallOrderService.deleteOrder(orderId);
        return success(result);
    }

    @Override
    @GetMapping("/statistics/sales-trend")
    public Response<List<Map<String, Object>>> getSalesTrend() {
        List<Map<String, Object>> trend = mallStatisticsService.getSalesTrend();
        return success(trend);
    }

    @Override
    @GetMapping("/statistics/category-ratio")
    public Response<List<Map<String, Object>>> getCategoryRatio() {
        List<Map<String, Object>> ratio = mallStatisticsService.getCategoryRatio();
        return success(ratio);
    }

    private Map<String, Object> toMap(Object obj) {
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(obj, Map.class);
    }
}