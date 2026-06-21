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

@Slf4j
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@Configurable
@EnableScheduling
@MapperScan("cn.fcr.infrastructure.dao")
public class Application {

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private PasswordEncoder passwordEncoder;

    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

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
