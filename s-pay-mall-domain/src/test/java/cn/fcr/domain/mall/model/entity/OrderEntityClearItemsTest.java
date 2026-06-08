package cn.fcr.domain.mall.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderEntity clearItems() 边界测试")
public class OrderEntityClearItemsTest {

    @Test
    @DisplayName("clearItems() 当 items 为 null 时，totalAmount 应重置为 BigDecimal.ZERO")
    public void clearItems_whenItemsIsNull_totalAmountShouldBeZero() {
        OrderEntity order = OrderEntity.builder()
                .items(null)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        assertEquals(0, order.getItems().size());
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());

        order.clearItems();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertEquals(0, order.getItems().size());
    }

    @Test
    @DisplayName("clearItems() 当 items 为空集合时，应正常执行不抛异常")
    public void clearItems_whenItemsIsEmptyList_shouldNotThrowException() {
        OrderEntity order = OrderEntity.builder()
                .items(Collections.emptyList())
                .totalAmount(new BigDecimal("50.00"))
                .build();

        assertEquals(0, order.getItems().size());
        assertEquals(new BigDecimal("50.00"), order.getTotalAmount());

        assertDoesNotThrow(() -> order.clearItems());

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertEquals(0, order.getItems().size());
    }

    @Test
    @DisplayName("clearItems() 后调用 addItem()，总金额计算应正确")
    public void clearItems_thenAddItem_totalAmountShouldBeCorrect() {
        OrderItemEntity item1 = OrderItemEntity.builder()
                .productId(1L)
                .productName("Test Product")
                .price(new BigDecimal("20.00"))
                .quantity(2)
                .build();

        OrderEntity order = OrderEntity.builder()
                .items(Collections.emptyList())
                .totalAmount(new BigDecimal("100.00"))
                .build();

        order.clearItems();
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());

        order.addItem(item1);

        BigDecimal expectedAmount = new BigDecimal("40.00");
        assertEquals(expectedAmount, order.getTotalAmount());
        assertEquals(1, order.getItems().size());
    }

    @Test
    @DisplayName("clearItems() 后多次调用 addItem()，总金额应累加正确")
    public void clearItems_thenAddMultipleItems_totalAmountShouldBeSum() {
        OrderItemEntity item1 = OrderItemEntity.builder()
                .productId(1L)
                .productName("Product A")
                .price(new BigDecimal("10.00"))
                .quantity(2)
                .build();

        OrderItemEntity item2 = OrderItemEntity.builder()
                .productId(2L)
                .productName("Product B")
                .price(new BigDecimal("15.00"))
                .quantity(3)
                .build();

        OrderEntity order = OrderEntity.builder()
                .items(Collections.emptyList())
                .totalAmount(new BigDecimal("100.00"))
                .build();

        order.clearItems();
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());

        order.addItem(item1);
        order.addItem(item2);

        BigDecimal expectedAmount = new BigDecimal("65.00");
        assertEquals(expectedAmount, order.getTotalAmount());
        assertEquals(2, order.getItems().size());
    }
}