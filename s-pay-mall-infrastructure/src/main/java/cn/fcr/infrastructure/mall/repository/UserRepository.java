package cn.fcr.infrastructure.mall.repository;

import cn.fcr.domain.mall.adapter.repository.IUserRepository;
import cn.fcr.domain.mall.model.entity.UserEntity;
import cn.fcr.infrastructure.dao.ICartItemDao;
import cn.fcr.infrastructure.dao.IMallUserDao;
import cn.fcr.infrastructure.dao.IUserBindingDao;
import cn.fcr.infrastructure.dao.IUserRoleDao;
import cn.fcr.infrastructure.dao.po.MallUser;
import cn.fcr.types.common.Constants;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserRepository implements IUserRepository {

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private ICartItemDao cartItemDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Override
    public UserEntity findByUsernameWithRole(String username) {
        MallUser user = mallUserDao.findByUsernameWithRole(username);
        return user != null ? toUserEntity(user) : null;
    }

    @Override
    public Integer countByUsername(String username) {
        return mallUserDao.countByUsername(username);
    }

    @Override
    public Long insert(String username, String password, Integer status) {
        MallUser user = new MallUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setStatus(status != null ? status : Constants.USER_STATUS_ACTIVE);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        mallUserDao.insert(user);
        return user.getId();
    }

    @Override
    public int updateStatus(Long userId, Integer status) {
        LambdaUpdateWrapper<MallUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MallUser::getId, userId);
        updateWrapper.set(MallUser::getStatus, status);
        updateWrapper.set(MallUser::getUpdateTime, LocalDateTime.now());
        return mallUserDao.update(null, updateWrapper);
    }

    @Override
    public int updateUser(UserEntity user) {
        MallUser mallUser = toMallUser(user);
        LambdaUpdateWrapper<MallUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MallUser::getId, mallUser.getId());
        updateWrapper.set(MallUser::getStatus, mallUser.getStatus() != null ? mallUser.getStatus() : Constants.USER_STATUS_ACTIVE);
        updateWrapper.set(MallUser::getUpdateTime, LocalDateTime.now());
        if (mallUser.getPassword() != null) {
            updateWrapper.set(MallUser::getPassword, mallUser.getPassword());
        }
        return mallUserDao.update(null, updateWrapper);
    }

    @Override
    public int deleteById(Long id) {
        return mallUserDao.deleteById(id);
    }

    @Override
    public UserEntity findById(Long userId) {
        MallUser user = mallUserDao.selectById(userId);
        return user != null ? toUserEntity(user) : null;
    }

    @Override
    public List<UserEntity> listUsersWithRole(String username, Integer status, String roleCode) {
        List<MallUser> users = mallUserDao.listUsersWithRole(username, status, roleCode);
        return users.stream()
                .map(user -> {
                    UserEntity entity = toUserEntity(user);
                    if (entity.getRoleCode() == null) {
                        entity.setRoleCode(Constants.DEFAULT_ROLE_MEMBER);
                    }
                    return entity;
                })
                .collect(Collectors.toList());
    }

    @Override
    public int insertUserRole(Long userId, Long roleId) {
        return userRoleDao.insertUserRole(userId, roleId);
    }

    @Override
    public int deleteUserRoleByUserId(Long userId) {
        return userRoleDao.deleteByUserId(userId);
    }

    @Override
    public int deleteUserBindingByUserId(Long userId) {
        return userBindingDao.deleteByUserId(userId);
    }

    @Override
    public int deleteCartItemByUserId(Long userId) {
        return cartItemDao.deleteByUserId(userId);
    }

    @Override
    public String getRoleCodeByUserId(Long userId) {
        return userRoleDao.getRoleCodeByUserId(userId);
    }

    private UserEntity toUserEntity(MallUser po) {
        return UserEntity.builder()
                .id(po.getId())
                .username(po.getUsername())
                .password(po.getPassword())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .roleCode(po.getRoleCode())
                .roleId(po.getRoleId())
                .build();
    }

    private MallUser toMallUser(UserEntity entity) {
        MallUser po = new MallUser();
        po.setId(entity.getId());
        po.setUsername(entity.getUsername());
        po.setPassword(entity.getPassword());
        po.setStatus(entity.getStatus());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateTime(entity.getUpdateTime());
        po.setRoleCode(entity.getRoleCode());
        po.setRoleId(entity.getRoleId());
        return po;
    }
}
