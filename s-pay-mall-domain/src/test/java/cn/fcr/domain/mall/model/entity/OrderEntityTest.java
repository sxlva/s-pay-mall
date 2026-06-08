package cn.fcr.domain.mall.model.entity;

import cn.fcr.domain.mall.model.valobj.CartItemVO;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * OrderEntity 单元测试
 * 验证订单状态流转、金额计算、工厂方法等核心业务逻辑
 */
public class OrderEntityTest {

    // ==================== 状态检查测试 ====================

    @Test
    public void testCanPay_InitState_ShouldReturnTrue() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .build();
        assertTrue(order.canPay());
    }

    @Test
    public void testCanPay_PaidState_ShouldReturnFalse() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        assertFalse(order.canPay());
    }

    @Test
    public void testCanCancel_InitState_ShouldReturnTrue() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .build();
        assertTrue(order.canCancel());
    }

    @Test
    public void testCanCancel_PaidState_ShouldReturnFalse() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        assertFalse(order.canCancel());
    }

    @Test
    public void testCanDeliver_PaidState_ShouldReturnTrue() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        assertTrue(order.canDeliver());
    }

    @Test
    public void testCanDeliver_InitState_ShouldReturnFalse() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .build();
        assertFalse(order.canDeliver());
    }

    @Test
    public void testCanComplete_ShippedState_ShouldReturnTrue() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.SHIPPED)
                .build();
        assertTrue(order.canComplete());
    }

    @Test
    public void testCanComplete_PaidState_ShouldReturnFalse() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        assertFalse(order.canComplete());
    }

    // ==================== 状态转换测试 ====================

    @Test
    public void testPay_InitState_ShouldChangeToPaid() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .updateTime(LocalDateTime.now().minusHours(1))
                .build();
        
        order.pay();
        
        assertEquals(OrderState.PAID, order.getState());
        assertNotNull(order.getUpdateTime());
    }

    @Test
    public void testPay_PaidState_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        
        try {
            order.pay();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("无法执行支付操作"));
        }
    }

    @Test
    public void testCancel_InitState_ShouldChangeToCanceled() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .updateTime(LocalDateTime.now().minusHours(1))
                .build();
        
        order.cancel();
        
        assertEquals(OrderState.CANCELED, order.getState());
    }

    @Test
    public void testCancel_PaidState_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        
        try {
            order.cancel();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("无法执行取消操作"));
        }
    }

    @Test
    public void testDeliver_PaidState_ShouldChangeToShipped() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .updateTime(LocalDateTime.now().minusHours(1))
                .build();
        
        order.deliver();
        
        assertEquals(OrderState.SHIPPED, order.getState());
    }

    @Test
    public void testDeliver_InitState_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.INIT)
                .build();
        
        try {
            order.deliver();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("无法执行发货操作"));
        }
    }

    @Test
    public void testComplete_ShippedState_ShouldChangeToDone() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.SHIPPED)
                .updateTime(LocalDateTime.now().minusHours(1))
                .build();
        
        order.complete();
        
        assertEquals(OrderState.DONE, order.getState());
    }

    @Test
    public void testComplete_PaidState_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .state(OrderState.PAID)
                .build();
        
        try {
            order.complete();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("无法执行完成操作"));
        }
    }

    @Test
    public void testPay_NullState_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .state(null)
                .build();
        
        try {
            order.pay();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("null"));
        }
    }

    // ==================== 金额计算测试 ====================

    @Test
    public void testCalculateTotalAmount_EmptyItems_ShouldReturnZero() {
        OrderEntity order = OrderEntity.builder()
                .items(new ArrayList<>())
                .build();
        
        assertEquals(BigDecimal.ZERO, order.calculateTotalAmount());
    }

    @Test
    public void testCalculateTotalAmount_NullItems_ShouldReturnZero() {
        OrderEntity order = OrderEntity.builder()
                .items(null)
                .build();
        
        assertEquals(BigDecimal.ZERO, order.calculateTotalAmount());
    }

    @Test
    public void testCalculateTotalAmount_SingleItem_ShouldReturnCorrectAmount() {
        OrderItemEntity item = OrderItemEntity.builder()
                .price(new BigDecimal("100.00"))
                .quantity(2)
                .build();
        
        OrderEntity order = OrderEntity.builder()
                .items(List.of(item))
                .build();
        
        assertEquals(new BigDecimal("200.00"), order.calculateTotalAmount());
    }

    @Test
    public void testCalculateTotalAmount_MultipleItems_ShouldReturnSum() {
        OrderItemEntity item1 = OrderItemEntity.builder()
                .price(new BigDecimal("100.00"))
                .quantity(2)
                .build();
        
        OrderItemEntity item2 = OrderItemEntity.builder()
                .price(new BigDecimal("50.00"))
                .quantity(3)
                .build();
        
        OrderEntity order = OrderEntity.builder()
                .items(List.of(item1, item2))
                .build();
        
        assertEquals(new BigDecimal("350.00"), order.calculateTotalAmount());
    }

    // ==================== 订单项管理测试 ====================

    @Test
    public void testAddItem_ValidItem_ShouldAddToList() {
        OrderEntity order = OrderEntity.builder()
                .items(new ArrayList<>())
                .build();
        
        OrderItemEntity item = OrderItemEntity.builder()
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("99.00"))
                .quantity(1)
                .build();
        
        order.addItem(item);
        
        assertEquals(1, order.getItems().size());
        assertEquals(item, order.getItems().get(0));
    }

    @Test
    public void testAddItem_NullItem_ShouldThrowException() {
        OrderEntity order = OrderEntity.builder()
                .items(new ArrayList<>())
                .build();
        
        try {
            order.addItem(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("订单项不能为空", e.getMessage());
        }
    }

    @Test
    public void testAddItem_NullItemList_ShouldCreateNewList() {
        OrderEntity order = OrderEntity.builder()
                .items(null)
                .build();
        
        OrderItemEntity item = OrderItemEntity.builder()
                .productId(1L)
                .productName("Test Product")
                .build();
        
        order.addItem(item);
        
        assertNotNull(order.getItems());
        assertEquals(1, order.getItems().size());
    }

    @Test
    public void testClearItems_HasItems_ShouldClearList() {
        OrderItemEntity item = OrderItemEntity.builder()
                .productId(1L)
                .build();
        
        List<OrderItemEntity> items = new ArrayList<>();
        items.add(item);
        OrderEntity order = OrderEntity.builder()
                .items(items)
                .build();
        
        order.clearItems();
        
        assertEquals(0, order.getItems().size());
    }

    // ==================== 静态工厂方法测试 ====================

    @Test
    public void testCreateFromCart_ValidCartItems_ShouldCreateOrder() {
        CartItemVO cartItem = CartItemVO.builder()
                .productId(1L)
                .productName("Test Product")
                .productPrice(new BigDecimal("100.00"))
                .quantity(2)
                .itemAmount(new BigDecimal("200.00"))
                .build();
        
        List<CartItemVO> cartItems = List.of(cartItem);
        
        OrderEntity order = OrderEntity.createFromCart(1L, "Test Address", cartItems);
        
        assertNotNull(order);
        assertNotNull(order.getOrderNo());
        assertTrue(order.getOrderNo().startsWith("ORD"));
        assertEquals(Long.valueOf(1L), order.getUserId());
        assertEquals("Test Address", order.getAddress());
        assertEquals(OrderState.INIT, order.getState());
        assertEquals(new BigDecimal("200.00"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
    }

    @Test
    public void testCreateFromCart_EmptyCart_ShouldThrowException() {
        try {
            OrderEntity.createFromCart(1L, "Address", Collections.emptyList());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("购物车为空", e.getMessage());
        }
    }

    @Test
    public void testCreateFromCart_NullCart_ShouldThrowException() {
        try {
            OrderEntity.createFromCart(1L, "Address", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("购物车为空", e.getMessage());
        }
    }

    @Test
    public void testCreateFromCart_MultipleItems_ShouldCalculateCorrectTotal() {
        CartItemVO item1 = CartItemVO.builder()
                .productId(1L)
                .productName("Product 1")
                .productPrice(new BigDecimal("100.00"))
                .quantity(2)
                .itemAmount(new BigDecimal("200.00"))
                .build();
        
        CartItemVO item2 = CartItemVO.builder()
                .productId(2L)
                .productName("Product 2")
                .productPrice(new BigDecimal("50.00"))
                .quantity(1)
                .itemAmount(new BigDecimal("50.00"))
                .build();
        
        OrderEntity order = OrderEntity.createFromCart(1L, "Address", List.of(item1, item2));
        
        assertEquals(new BigDecimal("250.00"), order.getTotalAmount());
        assertEquals(2, order.getItems().size());
    }

    // ==================== 辅助方法 ====================

    private void fail(String message) {
        throw new AssertionError(message);
    }
}