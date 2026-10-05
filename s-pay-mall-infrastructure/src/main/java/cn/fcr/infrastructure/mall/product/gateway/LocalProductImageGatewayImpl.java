package cn.fcr.infrastructure.mall.product.gateway;

import cn.fcr.domain.mall.product.gateway.IProductImageGateway;
import cn.fcr.domain.mall.product.model.valobj.ProductImageFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 商品图片本地磁盘存储网关实现
 *
 * <p>图片写入 ${app.config.upload-dir}/products/ 目录，
 * 返回 /uploads/products/ 相对路径，由 Spring 静态资源映射对外提供访问。</p>
 *
 * @author 傅崇睿
 */
@Component
public class LocalProductImageGatewayImpl implements IProductImageGateway {

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 上传文件根目录（绝对路径） */
    private final String uploadDir;

    public LocalProductImageGatewayImpl(@Value("${app.config.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String saveProductImage(byte[] data, String originalFilename) {
        String extension = resolveExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Path targetDir = Paths.get(uploadDir, "products");
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.write(target, data);
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("商品图片保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从原始文件名解析并校验扩展名
     *
     * @param originalFilename 原始文件名
     * @return 小写扩展名
     * @throws IllegalArgumentException 扩展名缺失或不在白名单内
     */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("图片文件名缺少扩展名");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的图片格式: " + extension + "，仅支持 jpg/jpeg/png/webp/gif");
        }
        return extension;
    }

    @Override
    public List<ProductImageFile> listProductImages() {
        Path dir = Paths.get(uploadDir, "products");
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(this::toImageFile)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("扫描商品图片目录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteProductImage(String filename) {
        Path target = resolveSafeTarget(filename);
        try {
            Files.delete(target);
        } catch (NoSuchFileException e) {
            // 文件已不存在（可能被并发清理删除），视为成功
        } catch (IOException e) {
            throw new IllegalStateException("删除商品图片失败: " + filename + ", " + e.getMessage(), e);
        }
    }

    /**
     * 读取文件创建时间（取不到时回退最后修改时间）
     *
     * @param path 文件路径
     * @return 文件描述对象
     */
    private ProductImageFile toImageFile(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime creationTime = attrs.creationTime();
            Instant createdAt = creationTime != null ? creationTime.toInstant()
                    : attrs.lastModifiedTime().toInstant();
            return new ProductImageFile(path.getFileName().toString(), createdAt);
        } catch (IOException e) {
            // 单文件读取失败（如扫描期间被删除）不中断整体扫描，按"极新"处理使其落入保护期，不会被清理
            return new ProductImageFile(path.getFileName().toString(), Instant.now());
        }
    }

    /**
     * 将文件名安全地约束在商品图片目录内
     *
     * <p>拒绝包含路径分隔符或 ".." 的非法文件名，防止路径穿越删除目录外的文件。</p>
     *
     * @param filename 文件名
     * @return 目录内目标路径
     * @throws IllegalArgumentException 文件名为空或含非法字符
     */
    private Path resolveSafeTarget(String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("非法图片文件名: " + filename);
        }
        return Paths.get(uploadDir, "products").resolve(filename);
    }
}
