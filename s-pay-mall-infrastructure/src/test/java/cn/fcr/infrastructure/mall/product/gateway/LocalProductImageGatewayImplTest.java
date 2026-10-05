package cn.fcr.infrastructure.mall.product.gateway;

import cn.fcr.domain.mall.product.model.valobj.ProductImageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品图片本地存储网关单元测试
 *
 * <p>锁定文件系统安全语义：
 * <ul>
 *   <li>保存的图片落在 products/ 子目录，返回 /uploads/products/ 相对路径</li>
 *   <li>列表包含非图片文件，由调用方分类处理</li>
 *   <li>删除拒绝路径穿越文件名（.. / 分隔符），删除不存在文件静默成功</li>
 * </ul>
 *
 * @author 傅崇睿
 */
class LocalProductImageGatewayImplTest {

    @TempDir
    Path tempDir;

    private LocalProductImageGatewayImpl gateway() {
        return new LocalProductImageGatewayImpl(tempDir.toString());
    }

    @Test
    void 保存图片写入products子目录并返回相对路径() {
        String url = gateway().saveProductImage(new byte[]{1, 2, 3}, "photo.PNG");

        assertTrue(url.startsWith("/uploads/products/"), "应返回 /uploads/products/ 相对路径: " + url);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        // 扩展名应统一为小写
        assertTrue(filename.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve("products").resolve(filename)));
    }

    @Test
    void 非白名单扩展名保存被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> gateway().saveProductImage(new byte[]{1}, "evil.exe"));
    }

    @Test
    void 列表返回全部文件含非图片文件() throws IOException {
        Files.createDirectories(tempDir.resolve("products"));
        Files.write(tempDir.resolve("products/a.jpg"), new byte[]{1});
        Files.write(tempDir.resolve("products/b.txt"), new byte[]{1});

        List<ProductImageFile> files = gateway().listProductImages();

        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.filename().equals("a.jpg")));
        assertTrue(files.stream().anyMatch(f -> f.filename().equals("b.txt")));
        // 创建时间非空
        assertTrue(files.stream().allMatch(f -> f.createdAt() != null));
    }

    @Test
    void 目录不存在时列表返回空() {
        assertTrue(gateway().listProductImages().isEmpty());
    }

    @Test
    void 删除拒绝路径穿越文件名() {
        assertThrows(IllegalArgumentException.class, () -> gateway().deleteProductImage("../a.jpg"));
        assertThrows(IllegalArgumentException.class, () -> gateway().deleteProductImage("a/b.jpg"));
        assertThrows(IllegalArgumentException.class, () -> gateway().deleteProductImage("a\\b.jpg"));
        assertThrows(IllegalArgumentException.class, () -> gateway().deleteProductImage(".."));
    }

    @Test
    void 删除不存在文件静默成功且不影响外部文件() throws IOException {
        // 目录外放一个文件，尝试用 ../ 之外的正常文件名删除，确认目录外文件不受影响
        Path outside = tempDir.resolve("outside.jpg");
        Files.write(outside, new byte[]{1});

        assertDoesNotThrow(() -> gateway().deleteProductImage("not-exist.jpg"));
        assertTrue(Files.exists(outside));
    }

    @Test
    void 正常删除已存在文件() throws IOException {
        Files.createDirectories(tempDir.resolve("products"));
        Files.write(tempDir.resolve("products/gone.jpg"), new byte[]{1});

        gateway().deleteProductImage("gone.jpg");

        assertFalse(Files.exists(tempDir.resolve("products/gone.jpg")));
    }
}
