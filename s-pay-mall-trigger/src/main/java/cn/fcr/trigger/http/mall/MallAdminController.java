package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.CategorySaveRequestDTO;
import cn.fcr.api.dto.ProductSaveRequest;
import cn.fcr.api.dto.UserSaveRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.api.vo.CategoryRatioVO;
import cn.fcr.api.vo.CategoryVO;
import cn.fcr.api.vo.OrderVO;
import cn.fcr.api.vo.ProductVO;
import cn.fcr.api.vo.SalesTrendVO;
import cn.fcr.api.vo.UserVO;
import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.model.entity.UserEntity;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.domain.mall.service.IMallStatisticsService;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 处理后台管理相关的接口，包括用户管理、商品管理、订单管理、统计分析等
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}/admin")
public class MallAdminController extends BaseController {

    @Resource
    private IMallUserService mallUserService;

    @Resource
    private IMallProductService mallProductService;

    @Resource
    private IMallOrderService mallOrderService;

    @Resource
    private IMallStatisticsService mallStatisticsService;

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Response<List<UserVO>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String roleCode) {
        List<UserEntity> users = mallUserService.listUsers(username, status, roleCode);
        List<UserVO> result = users.stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setStatus(u.getStatus());
            vo.setRoleCode(u.getRoleCode());
            vo.setRoleName(getRoleName(u.getRoleCode()));
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    private String getRoleName(String roleCode) {
        if ("ADMIN".equals(roleCode)) {
            return "管理员";
        } else if ("USER".equals(roleCode)) {
            return "普通会员";
        }
        return roleCode;
    }

    @PostMapping("/users")
    public Response<Integer> saveUser(@RequestBody UserSaveRequestDTO request) {
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

    @PutMapping("/users/{userId}/status")
    public Response<Integer> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status) {
        log.info("更新用户状态: id={}, status={}", userId, status);
        int result = mallUserService.updateUserStatus(userId, status);
        return success(result);
    }

    @DeleteMapping("/users/{userId}")
    public Response<Integer> deleteUser(@PathVariable Long userId) {
        log.info("删除用户: id={}", userId);
        int result = mallUserService.deleteUser(userId);
        return success(result);
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public Response<List<CategoryVO>> listCategories() {
        List<cn.fcr.domain.mall.model.valobj.CategoryVO> categories = mallProductService.listCategory();
        List<CategoryVO> result = categories.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    @PostMapping("/categories")
    public Response<Integer> saveCategory(@RequestBody CategorySaveRequestDTO request) {
        log.info("保存分类: name={}", request.getName());
        int result = mallProductService.saveCategory(toMap(request));
        return success(result);
    }

    @DeleteMapping("/categories/{categoryId}")
    public Response<Integer> deleteCategory(@PathVariable Long categoryId) {
        log.info("删除分类: id={}", categoryId);
        int result = mallProductService.deleteCategory(categoryId);
        return success(result);
    }

    // ==================== 商品管理 ====================

    @GetMapping("/products")
    public Response<List<ProductVO>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer status) {
        List<cn.fcr.domain.mall.model.valobj.ProductVO> products = mallProductService.listProducts(categoryId, null, keyword, minPrice, maxPrice, status);
        List<ProductVO> result = products.stream().map(p -> {
            ProductVO vo = new ProductVO();
            vo.setId(p.getId());
            vo.setCategoryId(p.getCategoryId());
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setPrice(p.getPrice());
            vo.setStock(p.getStock());
            vo.setCategory(p.getCategory());
            vo.setCategoryName(p.getCategoryName());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    @PostMapping("/products")
    public Response<Integer> saveProduct(@RequestBody ProductSaveRequest request) {
        log.info("保存商品: name={}", request.getName());
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

    @DeleteMapping("/products/{productId}")
    public Response<Integer> deleteProduct(@PathVariable Long productId) {
        log.info("删除商品: id={}", productId);
        int result = mallProductService.deleteProduct(productId);
        return success(result);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public Response<List<OrderVO>> listOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        List<cn.fcr.domain.mall.model.valobj.OrderVO> orders = mallOrderService.listOrders(userId, status, startTime, endTime);
        
        List<OrderVO> result = orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setUserId(o.getUserId());
            vo.setStatus(o.getStatus());
            vo.setStatusDesc(o.getStatusDesc() != null ? o.getStatusDesc() : getOrderStatusDesc(o.getStatus()));
            vo.setTotalAmount(o.getTotalAmount());
            vo.setAddress(o.getAddress() != null ? o.getAddress() : "");
            vo.setCreateTime(o.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        
        log.info("【订单查询】管理后台查询订单数量: {}", result.size());
        return success(result);
    }

    private String getOrderStatusDesc(String status) {
        OrderState state = OrderState.fromCode(status);
        return state != null ? state.getDescription() : status;
    }

    @PutMapping("/orders/{orderId}/deliver")
    public Response<Integer> deliverOrder(@PathVariable Long orderId) {
        log.info("一键发货: orderId={}", orderId);
        int result = mallOrderService.deliverOrder(orderId);
        return success(result);
    }

    @PutMapping("/orders/{orderId}/cancel")
    public Response<Integer> cancelOrder(@PathVariable Long orderId) {
        log.info("取消订单: orderId={}", orderId);
        int result = mallOrderService.cancelOrder(orderId);
        return success(result);
    }

    @DeleteMapping("/orders/{orderId}")
    public Response<Integer> deleteOrder(@PathVariable Long orderId) {
        log.info("删除订单: id={}", orderId);
        int result = mallOrderService.deleteOrder(orderId);
        return success(result);
    }

    // ==================== 统计分析 ====================

    @GetMapping("/statistics/sales-trend")
    public Response<List<SalesTrendVO>> getSalesTrend() {
        List<Map<String, Object>> trend = mallStatisticsService.getSalesTrend();
        List<SalesTrendVO> result = trend.stream().map(map -> {
            SalesTrendVO vo = new SalesTrendVO();
            vo.setDate((String) map.get("date"));
            vo.setSalesAmount(toBigDecimal(map.get("salesAmount")));
            vo.setOrderCount((Integer) map.get("orderCount"));
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    @GetMapping("/statistics/category-ratio")
    public Response<List<CategoryRatioVO>> getCategoryRatio() {
        List<Map<String, Object>> ratio = mallStatisticsService.getCategoryRatio();
        List<CategoryRatioVO> result = ratio.stream().map(map -> {
            CategoryRatioVO vo = new CategoryRatioVO();
            vo.setCategoryName((String) map.get("name"));
            vo.setProductCount(((Number) map.get("product_count")).intValue());
            vo.setSalesAmount(toBigDecimal(map.get("sales_amount")));
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private java.util.Map<String, Object> toMap(Object obj) {
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(obj, java.util.Map.class);
    }
}
