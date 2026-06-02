package cn.fcr.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载 .env 文件中的环境变量
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String projectRoot = System.getProperty("user.dir");
        log.info("【DotenvEnvironmentPostProcessor】项目根目录: {}", projectRoot);

        File envFile = new File(projectRoot, ".env");
        log.info("【DotenvEnvironmentPostProcessor】尝试加载 .env 文件: {}", envFile.getAbsolutePath());

        Dotenv dotenv;
        if (envFile.exists()) {
            log.info("【DotenvEnvironmentPostProcessor】✅ 找到 .env 文件，路径: {}", envFile.getAbsolutePath());
            dotenv = Dotenv.configure()
                    .directory(projectRoot)
                    .filename(".env")
                    .load();
        } else {
            log.warn("【DotenvEnvironmentPostProcessor】❌ 项目根目录下未找到 .env 文件，尝试加载 classpath 中的配置");
            dotenv = Dotenv.load();
        }

        Map<String, Object> envMap = new HashMap<>();

        dotenv.entries().forEach(entry -> {
            envMap.put(entry.getKey(), entry.getValue());
        });

        log.info("【DotenvEnvironmentPostProcessor】成功加载 {} 个环境变量配置项", envMap.size());
        log.info("【DotenvEnvironmentPostProcessor】当前成功加载的 .env 文件绝对路径为: {}", envFile.getAbsolutePath());

        if (envMap.containsKey("MYSQL_ROOT_PASSWORD")) {
            log.info("【DotenvEnvironmentPostProcessor】✅ MYSQL_ROOT_PASSWORD 已成功注入，值为: ****");
        } else {
            log.warn("【DotenvEnvironmentPostProcessor】❌ MYSQL_ROOT_PASSWORD 未在 .env 中找到");
        }

        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", envMap));
    }
}