package cn.fcr.trigger.http;

import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.trigger.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 支付宝同步回调Controller
 *
 * @author 傅崇睿
 */
@Slf4j
@Controller
@RequestMapping("/orders")
public class AliPayReturnController {

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /**
     * 处理支付宝支付同步跳转
     *
     * <p>支付宝支付完成后浏览器同步跳回此地址。查询订单状态后重定向到商城主页。
     * 返回层面无实际业务变更，仅记录订单状态方便排查。</p>
     *
     * @param orderNo     商户订单号
     * @param tradeNo     支付宝交易号
     * @param totalAmount 支付金额
     * @param request     HTTP请求
     * @return 重定向到商城主页的视图
     */
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
                OrderVO orderVO = orderApplicationService.getOrderByNo(orderNo);
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
