package cn.fcr.trigger.http;

import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/orders")
public class AliPayReturnController {

    @Resource
    private IMallOrderService mallOrderService;

    @GetMapping
    public ModelAndView handleAlipayReturn(
            @RequestParam(value = "out_trade_no", required = false) String orderNo,
            @RequestParam(value = "trade_no", required = false) String tradeNo,
            @RequestParam(value = "total_amount", required = false) String totalAmount,
            HttpServletRequest request) {
        
        log.info("【紧急调试】请求已进入 AliPayReturnController，完整路径: {}, 请求方法: {}", 
                request.getRequestURI(), request.getMethod());
        log.info("支付宝同步回调 - out_trade_no: {}, trade_no: {}, total_amount: {}", 
                orderNo, tradeNo, totalAmount);
        
        if (orderNo != null && !orderNo.isEmpty()) {
            try {
                OrderVO orderVO = mallOrderService.getOrderByNo(orderNo);
                if (orderVO != null) {
                    log.info("订单查询成功 - orderNo: {}, status: {}", orderNo, orderVO.getStatus());
                }
            } catch (Exception e) {
                log.error("查询订单失败: orderNo={}", orderNo, e);
            }
        }
        
        log.info("重定向到商城主页: https://fuchongrui.site");
        return new ModelAndView("redirect:https://fuchongrui.site");
    }
}