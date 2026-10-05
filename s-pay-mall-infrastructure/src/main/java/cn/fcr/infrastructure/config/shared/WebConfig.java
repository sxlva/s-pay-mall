package cn.fcr.infrastructure.config.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：上传图片的静态资源映射
 *
 * <p>将 /uploads/** 请求映射到本地磁盘 ${app.config.upload-dir}/ 目录，
 * 使商品图片可直接通过相对路径访问，不经过业务 Controller。</p>
 *
 * @author 傅崇睿
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 上传文件根目录（绝对路径） */
    private final String uploadDir;

    public WebConfig(@Value("${app.config.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
