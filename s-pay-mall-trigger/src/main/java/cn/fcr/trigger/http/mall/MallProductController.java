package cn.fcr.trigger.http.mall;

import cn.fcr.api.IMallProductService;
import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.model.valobj.CategoryVO;
import cn.fcr.domain.mall.model.valobj.ProductVO;
import cn.fcr.trigger.http.BaseController;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品控制器
 * 实现 IMallProductService 接口，处理商品查询、分类查询等接口
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class MallProductController extends BaseController implements IMallProductService {

    @Resource
    private cn.fcr.domain.mall.service.IMallProductService mallProductService;

    @Override
    @GetMapping("/products")
    public Response<List<Map<String, Object>>> listProducts(
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer status) {
        log.info("商品列表查询: categoryId={}, keyword={}", categoryId, keyword);
        List<ProductVO> products = mallProductService.listProducts(categoryId, null, keyword, minPrice, maxPrice, status);
        List<Map<String, Object>> result = products.stream()
                .map(p -> (Map<String, Object>) JSON.parse(JSON.toJSONString(p)))
                .collect(Collectors.toList());
        return success(result);
    }

    @Override
    @GetMapping("/categories")
    public Response<List<Map<String, Object>>> listCategories() {
        log.info("商品分类列表查询");
        List<CategoryVO> categories = mallProductService.listCategory();
        List<Map<String, Object>> result = categories.stream()
                .map(c -> (Map<String, Object>) JSON.parse(JSON.toJSONString(c)))
                .collect(Collectors.toList());
        return success(result);
    }
}