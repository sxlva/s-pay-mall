package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商品分类数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供分类的 CRUD 操作
 * 自定义方法支持获取各分类下的商品数量统计
 */
@Mapper
public interface ICategoryDao extends BaseMapper<Category> {

    /**
     * 查询所有启用的分类及其商品数量
     * 返回分类名称和对应的商品数量，按商品数量降序排列
     * 用于前端分类侧边栏或首页分类展示
     *
     * @return 分类名称与商品数量的键值对列表
     */
    @Select("SELECT c.name, COUNT(p.id) as product_count FROM category c LEFT JOIN product p ON c.id = p.category_id WHERE c.status = 1 GROUP BY c.id, c.name ORDER BY product_count DESC")
    List<Map<String, Object>> getCategoryProductCount();
}