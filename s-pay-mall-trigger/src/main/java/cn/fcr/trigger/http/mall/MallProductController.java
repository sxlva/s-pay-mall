package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.api.vo.CategoryVO;
import cn.fcr.api.vo.ProductVO;
import cn.fcr.domain.mall.service.IMallProductService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品Controller
 *
 * <p>【DDD 触发层】处理商品查询、分类查询等接口。
 * 支持多条件筛选：分类、关键词、价格区间、状态。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class MallProductController extends BaseController {

    /** 商城商品领域服务 */
    @Resource
    private IMallProductService mallProductService;

    /**
     * 查询商品列表
     *
     * <p>支持多条件筛选：分类、关键词、价格区间、状态</p>
     *
     * @param categoryId 分类ID（可选）
     * @param keyword    关键词（可选）
     * @param minPrice   最低价（可选）
     * @param maxPrice   最高价（可选）
     * @param status     状态（可选）
     * @return 商品列表
     */
    @GetMapping("/products")
    public Response<List<ProductVO>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer status) {
        log.info("商品列表查询: categoryId={}, keyword={}", categoryId, keyword);
        List<cn.fcr.domain.mall.model.valobj.ProductVO> products = mallProductService.listProducts(categoryId, null, keyword, minPrice, maxPrice, status);
        List<ProductVO> result = products.stream().map(p -> {
            ProductVO vo = new ProductVO();
            vo.setId(p.getId());
            vo.setCategoryId(p.getCategoryId());
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setPrice(p.getPrice());
            vo.setStock(p.getStock());
            vo.setCategory(p.getCategory());
            vo.setCategoryName(p.getCategoryName());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    /**
     * 查询商品分类列表
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public Response<List<CategoryVO>> listCategories() {
        log.info("商品分类列表查询");
        List<cn.fcr.domain.mall.model.valobj.CategoryVO> categories = mallProductService.listCategory();
        List<CategoryVO> result = categories.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }
}
