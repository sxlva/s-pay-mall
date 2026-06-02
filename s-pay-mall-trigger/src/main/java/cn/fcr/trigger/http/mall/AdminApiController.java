package cn.fcr.trigger.http.mall;

import cn.fcr.api.IAdminService;
import cn.fcr.api.dto.CategorySaveRequest;
import cn.fcr.api.dto.ProductSaveRequest;
import cn.fcr.api.dto.UserSaveRequest;
import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.domain.mall.service.IMallStatisticsService;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端API控制器
 * 实现 IAdminService 接口，提供用户管理、商品管理、订单管理、统计分析等后台管理接口
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/pay-api/${app.config.api-version}/admin")
public class AdminApiController extends BaseController implements IAdminService {

    @Resource
    private IMallUserService mallUserService;

    @Resource
    private IMallProductService mallProductService;

    @Resource
    private IMallOrderService mallOrderService;

    @Resource
    private IMallStatisticsService mallStatisticsService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<Map<String, Object>>> listUsers(String username, Integer status, String roleCode) {
        List<Map<String, Object>> users = mallUserService.listUsers(username, status, roleCode);
        return success(users);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> saveUser(UserSaveRequest request) {
        log.info("保存用户: username={}", request.getUsername());
        int result = mallUserService.saveUser(toMap(request));
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> updateUserStatus(Long userId, Integer status) {
        log.info("更新用户状态: id={}, status={}", userId, status);
        int result = mallUserService.updateUserStatus(userId, status);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> deleteUser(Long userId) {
        log.info("删除用户: id={}", userId);
        int result = mallUserService.deleteUser(userId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> saveCategory(CategorySaveRequest request) {
        log.info("保存分类: name={}", request.getName());
        int result = mallProductService.saveCategory(toMap(request));
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> deleteCategory(Long categoryId) {
        log.info("删除分类: id={}", categoryId);
        int result = mallProductService.deleteCategory(categoryId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<Map<String, Object>>> listProducts(
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer status) {
        List<ProductVO> products = mallProductService.listProducts(categoryId, null, keyword, minPrice, maxPrice, status);
        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("price", p.getPrice());
            map.put("stock", p.getStock());
            map.put("status", p.getStatus());
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> saveProduct(ProductSaveRequest request) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> deleteProduct(Long productId) {
        log.info("删除商品: id={}", productId);
        int result = mallProductService.deleteProduct(productId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<Map<String, Object>>> listOrders(Long userId, String status, String startTime, String endTime) {
        List<OrderVO> orders = mallOrderService.listOrders(userId, status, startTime, endTime);
        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("orderId", o.getOrderNo());
            map.put("status", o.getStatus());
            map.put("totalAmount", o.getTotalAmount());
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> deliverOrder(Long orderId) {
        log.info("一键发货: orderId={}", orderId);
        int result = mallOrderService.deliverOrder(orderId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> cancelOrder(Long orderId) {
        log.info("取消订单: orderId={}", orderId);
        int result = mallOrderService.cancelOrder(orderId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Integer> deleteOrder(Long orderId) {
        log.info("删除订单: id={}", orderId);
        int result = mallOrderService.deleteOrder(orderId);
        return success(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<Map<String, Object>>> getSalesTrend() {
        List<Map<String, Object>> trend = mallStatisticsService.getSalesTrend();
        return success(trend);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<Map<String, Object>>> getCategoryRatio() {
        List<Map<String, Object>> ratio = mallStatisticsService.getCategoryRatio();
        return success(ratio);
    }

    private Map<String, Object> toMap(Object obj) {
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(obj, Map.class);
    }
}