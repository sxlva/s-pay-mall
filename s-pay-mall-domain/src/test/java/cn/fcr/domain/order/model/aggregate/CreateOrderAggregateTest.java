package cn.fcr.domain.order.model.aggregate;

import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CreateOrderAggregate 单元测试
 * 验证订单聚合的静态工厂方法和对象组装逻辑
 */
public class CreateOrderAggregateTest {

    // ==================== 静态工厂方法测试 ====================

    @Test
    public void testBuildOrderEntity_ValidParams_ShouldCreateOrderEntity() {
        String productId = "PROD001";
        String productName = "Test Product";
        
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(productId, productName);
        
        assertNotNull(orderEntity);
        assertEquals(productId, orderEntity.getProductId());
        assertEquals(productName, orderEntity.getProductName());
        assertNotNull(orderEntity.getOrderId());
        assertEquals(14, orderEntity.getOrderId().length());
        assertNotNull(orderEntity.getOrderTime());
        assertEquals(OrderStatusVO.CREATE, orderEntity.getOrderStatusVO());
    }

    @Test
    public void testBuildOrderEntity_NullProductId_ShouldStillCreate() {
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(null, "Test Product");
        
        assertNotNull(orderEntity);
        assertNull(orderEntity.getProductId());
        assertEquals("Test Product", orderEntity.getProductName());
    }

    @Test
    public void testBuildOrderEntity_NullProductName_ShouldStillCreate() {
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity("PROD001", null);
        
        assertNotNull(orderEntity);
        assertEquals("PROD001", orderEntity.getProductId());
        assertNull(orderEntity.getProductName());
    }

    @Test
    public void testBuildOrderEntity_EmptyProductId_ShouldCreateWithEmptyId() {
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity("", "Test Product");
        
        assertNotNull(orderEntity);
        assertEquals("", orderEntity.getProductId());
    }

    @Test
    public void testBuildOrderEntity_GeneratesUniqueOrderId() {
        OrderEntity order1 = CreateOrderAggregate.buildOrderEntity("PROD001", "Product 1");
        OrderEntity order2 = CreateOrderAggregate.buildOrderEntity("PROD002", "Product 2");
        
        assertNotNull(order1.getOrderId());
        assertNotNull(order2.getOrderId());
        assertNotEquals(order1.getOrderId(), order2.getOrderId());
    }

    @Test
    public void testBuildOrderEntity_OrderIdIsNumeric() {
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity("PROD001", "Test Product");
        
        String orderId = orderEntity.getOrderId();
        assertTrue(orderId.matches("[0-9]+"));
    }

    @Test
    public void testBuildOrderEntity_OrderStatusIsCreate() {
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity("PROD001", "Test Product");
        
        assertEquals(OrderStatusVO.CREATE, orderEntity.getOrderStatusVO());
        assertEquals("CREATE", orderEntity.getOrderStatusVO().getCode());
    }

    // ==================== 聚合对象构建测试 ====================

    @Test
    public void testCreateOrderAggregateBuilder_ShouldBuildCompleteAggregate() {
        cn.fcr.domain.order.model.entity.ProductEntity productEntity = 
                cn.fcr.domain.order.model.entity.ProductEntity.builder()
                        .productId("PROD001")
                        .productName("Test Product")
                        .price(new java.math.BigDecimal("99.99"))
                        .build();
        
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity("PROD001", "Test Product");
        
        CreateOrderAggregate aggregate = CreateOrderAggregate.builder()
                .userId("USER001")
                .productEntity(productEntity)
                .orderEntity(orderEntity)
                .build();
        
        assertNotNull(aggregate);
        assertEquals("USER001", aggregate.getUserId());
        assertNotNull(aggregate.getProductEntity());
        assertNotNull(aggregate.getOrderEntity());
        assertEquals("PROD001", aggregate.getProductEntity().getProductId());
        assertEquals("PROD001", aggregate.getOrderEntity().getProductId());
    }

    @Test
    public void testCreateOrderAggregate_AllFieldsNull_ShouldBuild() {
        CreateOrderAggregate aggregate = CreateOrderAggregate.builder().build();
        
        assertNotNull(aggregate);
        assertNull(aggregate.getUserId());
        assertNull(aggregate.getProductEntity());
        assertNull(aggregate.getOrderEntity());
    }

    // ==================== 辅助方法 ====================

    private void fail(String message) {
        throw new AssertionError(message);
    }
}