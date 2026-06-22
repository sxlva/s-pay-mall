package cn.fcr.domain.mall.model.entity;

import cn.fcr.domain.mall.model.exception.CategoryException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商城分类实体（充血模型）
 *
 * <p>业务逻辑封装在 Entity 内部，支持父子层级结构（parentId），
 * 状态管理 0=禁用/1=启用，层级限制最多 3 级。</p>
 *
 * @author 傅崇睿
 */
@Getter // 【重构】仅允许外部读取，状态变更通过行为方法
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryEntity {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 父分类ID（null 或 0 表示根分类）
     */
    @Builder.Default
    private Long parentId = 0L;

    /**
     * 分类层级（从1开始，根分类为1）
     */
    @Builder.Default
    private Integer level = 1;

    /**
     * 子分类数量（缓存字段，用于快速判断）
     */
    @Builder.Default
    private Integer childCount = 0;

    /**
     * 关联商品数量（缓存字段，用于快速判断）
     */
    @Builder.Default
    private Integer productCount = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 最大层级限制
     */
    public static final int MAX_LEVEL = 3;

    /**
     * 状态常量
     */
    public static final Integer STATUS_DISABLED = 0;
    public static final Integer STATUS_ACTIVE = 1;

    /* ==========================================
     * 状态校验方法
     * ========================================== */

    /**
     * 判断分类是否活跃（启用状态）
     * 【DDD 守卫方法】替代 Service 层的 if (status == 1) 判断
     *
     * @return true=活跃，false=禁用
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /**
     * 判断分类是否禁用
     *
     * @return true=禁用，false=活跃
     */
    public boolean isDisabled() {
        return STATUS_DISABLED.equals(status);
    }

    /* ==========================================
     * 层级处理方法
     * ========================================== */

    /**
     * 判断是否为根分类（没有父分类）
     *
     * @return true=根分类，false=子分类
     */
    public boolean isRoot() {
        return parentId == null || parentId == 0L;
    }

    /**
     * 判断是否为叶子分类（没有子分类）
     *
     * @return true=叶子分类，false=有子分类
     */
    public boolean isLeaf() {
        return childCount == null || childCount == 0;
    }

    /**
     * 判断是否可以添加子分类
     * 【守卫校验】检查层级是否达到上限
     *
     * @return true=可以添加，false=层级已满
     */
    public boolean canAddSubCategory() {
        return level != null && level < MAX_LEVEL;
    }

    /**
     * 获取下一级的层级值
     *
     * @return 子分类的层级
     */
    public int getNextLevel() {
        return level != null ? level + 1 : 2;
    }

    /* ==========================================
     * 行为方法（状态变更）
     * ========================================== */

    /**
     * 启用分类
     * 【守卫校验】如果已经活跃则抛出异常
     */
    public void activate() {
        if (isActive()) {
            throw new CategoryException(CategoryException.ErrorCode.CATEGORY_INACTIVE,
                    "分类 [" + getNameSafe() + "] 已经是活跃状态");
        }
        this.status = STATUS_ACTIVE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 禁用分类
     * 【守卫校验】如果已经禁用则抛出异常
     */
    public void deactivate() {
        if (isDisabled()) {
            throw new CategoryException(CategoryException.ErrorCode.CATEGORY_INACTIVE,
                    "分类 [" + getNameSafe() + "] 已经是禁用状态");
        }
        this.status = STATUS_DISABLED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 验证分类名称
     * 【守卫校验】检查名称是否合法
     *
     * @throws CategoryException 名称不合法时抛出
     */
    public void validateName() {
        if (name == null || name.trim().isEmpty()) {
            throw new CategoryException(CategoryException.ErrorCode.EMPTY_CATEGORY_NAME);
        }
        if (name.length() > 100) {
            throw new CategoryException(CategoryException.ErrorCode.CATEGORY_NAME_TOO_LONG);
        }
    }

    /**
     * 判断是否可以删除分类
     * 【守卫校验】检查是否存在子分类或关联商品
     *
     * @return true=可以删除，false=不可以删除
     */
    public boolean canDelete() {
        return isLeaf() && (productCount == null || productCount == 0);
    }

    /**
     * 准备删除分类
     * 【守卫校验】如果不能删除则抛出异常
     *
     * @throws CategoryException 无法删除时抛出
     */
    public void prepareDelete() {
        if (!isLeaf()) {
            throw new CategoryException(CategoryException.ErrorCode.CATEGORY_HAS_CHILDREN,
                    getNameSafe());
        }
        if (productCount != null && productCount > 0) {
            throw new CategoryException(CategoryException.ErrorCode.CATEGORY_HAS_PRODUCTS,
                    getNameSafe());
        }
    }

    /**
     * 准备创建子分类
     * 【守卫校验】检查层级限制
     *
     * @throws CategoryException 层级超过限制时抛出
     */
    public void prepareAddSubCategory() {
        if (!canAddSubCategory()) {
            throw new CategoryException(CategoryException.ErrorCode.MAX_LEVEL_EXCEEDED,
                    "当前层级: " + level + ", 最大层级: " + MAX_LEVEL);
        }
    }

    /* ==========================================
     * 辅助方法
     * ========================================== */

    /**
     * 安全获取分类名称（防止 NPE）
     *
     * @return 分类名称，若为空返回"未命名分类"
     */
    public String getNameSafe() {
        return name != null ? name : "未命名分类";
    }

    /**
     * 安全获取层级值（防止 NPE）
     *
     * @return 层级值，若为空返回1
     */
    public int getLevelSafe() {
        return level != null ? level : 1;
    }

    /**
     * 设置子分类数量（内部使用）
     * 【DDD 聚合根保护】仅允许内部或特定场景修改
     *
     * @param childCount 子分类数量
     */
    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    /**
     * 设置商品数量（内部使用）
     * 【DDD 聚合根保护】仅允许内部或特定场景修改
     *
     * @param productCount 商品数量
     */
    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }
}
