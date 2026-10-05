package cn.fcr.application;

import cn.fcr.domain.mall.product.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.product.gateway.IProductImageGateway;
import cn.fcr.domain.mall.product.model.valobj.ProductImageFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品图片清理应用服务单元测试
 *
 * <p>锁定清理语义：
 * <ul>
 *   <li>被引用且过保护期 → 保留</li>
 *   <li>未引用且过保护期 → 删除</li>
 *   <li>未引用但创建时间在保护期内 → 跳过（防误删刚上传未保存的图片）</li>
 *   <li>非图片扩展名 → 跳过，不参与删除</li>
 *   <li>数据库中 ../ 路径穿越引用 → 忽略，不参与比对</li>
 *   <li>单文件删除失败 → 不中断，计入失败数</li>
 * </ul>
 *
 * @author 傅崇睿
 */
@ExtendWith(MockitoExtension.class)
class ProductImageCleanupServiceTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IProductImageGateway productImageGateway;

    @InjectMocks
    private ProductImageCleanupService productImageCleanupService;

    @BeforeEach
    void setUp() throws Exception {
        // 保护期 24 小时（@Value 字段在单元测试中通过反射注入）
        Field field = ProductImageCleanupService.class.getDeclaredField("protectionHours");
        field.setAccessible(true);
        field.setInt(productImageCleanupService, 24);
    }

    private ProductImageFile file(String filename, Instant createdAt) {
        return new ProductImageFile(filename, createdAt);
    }

    @Test
    void 只删除未引用且超过保护期的图片() {
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);

        when(productRepository.selectAllImageUrls())
                .thenReturn(Set.of("/uploads/products/ref-old.jpg", "/uploads/products/ref-recent.png"));
        when(productImageGateway.listProductImages()).thenReturn(List.of(
                file("ref-old.jpg", old),          // 被引用 + 过保护期 → 保留
                file("ref-recent.png", recent),    // 被引用 + 保护期内 → 保留
                file("orphan-old.webp", old),      // 未引用 + 过保护期 → 删除
                file("orphan-recent.gif", recent)  // 未引用 + 保护期内 → 跳过
        ));

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        verify(productImageGateway).deleteProductImage("orphan-old.webp");
        verify(productImageGateway, never()).deleteProductImage("ref-old.jpg");
        verify(productImageGateway, never()).deleteProductImage("ref-recent.png");
        verify(productImageGateway, never()).deleteProductImage("orphan-recent.gif");
        assertEquals(4, summary.scanned());
        assertEquals(2, summary.referenced());
        assertEquals(1, summary.deleted());
        assertEquals(1, summary.protectedSkipped());
        assertEquals(0, summary.nonImageSkipped());
        assertEquals(0, summary.failed());
    }

    @Test
    void 非图片文件跳过且不参与删除() {
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        when(productRepository.selectAllImageUrls()).thenReturn(Set.of());
        when(productImageGateway.listProductImages()).thenReturn(List.of(
                file("notes.txt", old),
                file("no-extension", old),
                file("orphan-old.jpg", old)
        ));

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        verify(productImageGateway).deleteProductImage("orphan-old.jpg");
        verify(productImageGateway, never()).deleteProductImage("notes.txt");
        verify(productImageGateway, never()).deleteProductImage("no-extension");
        assertEquals(3, summary.scanned());
        assertEquals(1, summary.deleted());
        assertEquals(2, summary.nonImageSkipped());
    }

    @Test
    void 异常路径引用按保守策略视为已引用不删除() {
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        // 数据库中混入带 ../ 的异常路径：取其纯文件名后按"可能被引用"保守处理，不删除对应文件
        when(productRepository.selectAllImageUrls())
                .thenReturn(Set.of("/uploads/products/../../evil.jpg"));
        when(productImageGateway.listProductImages()).thenReturn(List.of(
                file("evil.jpg", old)
        ));

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        verify(productImageGateway, never()).deleteProductImage("evil.jpg");
        assertEquals(1, summary.referenced());
        assertEquals(0, summary.deleted());
    }

    @Test
    void 纯文件名为点点的路径被直接拒绝() {
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        // url 以 "/.." 结尾：提取出的文件名就是 ".."，必须被拒绝，不参与比对
        when(productRepository.selectAllImageUrls())
                .thenReturn(Set.of("/uploads/products/.."));
        when(productImageGateway.listProductImages()).thenReturn(List.of(
                file("orphan-old.jpg", old)
        ));

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        assertEquals(0, summary.referenced());
        verify(productImageGateway).deleteProductImage("orphan-old.jpg");
    }

    @Test
    void 单文件删除失败不中断整体清理() {
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        when(productRepository.selectAllImageUrls()).thenReturn(Set.of());
        when(productImageGateway.listProductImages()).thenReturn(List.of(
                file("fail.jpg", old),
                file("ok.jpg", old)
        ));
        doThrow(new IllegalStateException("磁盘只读")).when(productImageGateway).deleteProductImage("fail.jpg");

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        verify(productImageGateway).deleteProductImage("fail.jpg");
        verify(productImageGateway).deleteProductImage("ok.jpg");
        assertEquals(1, summary.deleted());
        assertEquals(1, summary.failed());
    }

    @Test
    void 目录为空时不做任何删除() {
        when(productRepository.selectAllImageUrls()).thenReturn(Set.of());
        when(productImageGateway.listProductImages()).thenReturn(List.of());

        ProductImageCleanupService.CleanupSummary summary = productImageCleanupService.cleanupOrphanProductImages();

        assertEquals(0, summary.scanned());
        assertEquals(0, summary.deleted());
    }
}
