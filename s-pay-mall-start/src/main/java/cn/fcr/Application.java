package cn.fcr;

import cn.fcr.infrastructure.dao.auth.IMallUserDao;
import cn.fcr.infrastructure.dao.auth.IUserRoleDao;
import cn.fcr.infrastructure.dao.auth.po.MallUser;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Spring Boot 应用启动类
 *
 * <p>【应用层入口】负责 Spring Boot 应用启动、MyBatis 扫描、定时任务启用、
 * 以及默认管理员账号初始化。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@Configurable
@EnableScheduling
@MapperScan("cn.fcr.infrastructure.dao")
public class Application {

    /** 商城用户 DAO */
    @Resource
    private IMallUserDao mallUserDao;

    /** 用户角色关联 DAO */
    @Resource
    private IUserRoleDao userRoleDao;

    /** 密码编码器 */
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

    /**
     * 初始化默认管理员账号
     *
     * <p>应用启动后检查是否存在 admin 用户，若不存在则创建
     * 默认管理员账号（admin / 123456）并分配 ADMIN 角色。</p>
     *
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner initDefaultAdmin() {
        return args -> {
            // 检查是否存在admin用户
            Integer count = mallUserDao.countByUsername("admin");
            if (count == null || count == 0) {
                log.info("系统初始化：创建默认管理员账号");

                // 创建管理员用户
                MallUser admin = new MallUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setStatus(1);
                admin.setCreateTime(LocalDateTime.now());
                admin.setUpdateTime(LocalDateTime.now());

                mallUserDao.insert(admin);

                // 关联管理员角色（ADMIN角色ID为1）
                userRoleDao.insertUserRole(admin.getId(), 1L);

                log.info("默认管理员账号创建成功：username=admin, password=123456");
            } else {
                log.info("系统已存在管理员账号，跳过初始化");
            }
        };
    }
}
