package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.valobj.UserLoginVO;

import java.util.List;
import java.util.Map;

/**
 * 用户领域服务接口
 */
public interface IMallUserService {
    
    UserLoginVO register(String username, String password);
    
    UserLoginVO registerWithWeChat(String username, String password, String openId);
    
    UserLoginVO login(String username, String password);
    
    List<Map<String, Object>> listUsers(String username, Integer status, String roleCode);
    
    int saveUser(Map<String, Object> user);
    
    int updateUserStatus(Long userId, Integer status);
    
    int deleteUser(Long id);
    
    Map<String, Object> getProfile(Long userId);
}