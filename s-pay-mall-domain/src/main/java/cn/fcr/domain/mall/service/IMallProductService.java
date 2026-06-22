package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.command.ProductSaveCommand;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品领域服务接口，定义商品分类管理和商品 CRUD 的抽象。
 *
 * @author 傅崇睿
 */
public interface IMallProductService {

    List<CategoryVO> listCategory();

    int saveCategory(Map<String, Object> category);

    int deleteCategory(Long id);

    List<ProductVO> listProducts(Long categoryId, String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer status);

    int saveProduct(ProductSaveCommand command);

    int deleteProduct(Long id);
}