package cn.fcr.trigger.http;

import cn.fcr.domain.auth.service.ILoginService;
import cn.fcr.domain.auth.service.WeixinBindService;
import cn.fcr.types.sdk.weixin.MessageTextEntity;
import cn.fcr.types.sdk.weixin.SignatureUtil;
import cn.fcr.types.sdk.weixin.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信服务对接，对接地址：<a href="https://fuchongrui.site/pay-api/v1/weixin/portal/receive">/pay-api/v1/weixin/portal/receive</a>
 * <p>
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/pay-api/${app.config.api-version}/weixin/portal/")
public class WeixinPortalController {

    @Value("${weixin.config.originalid}")
    private String originalid;
    @Value("${weixin.config.token}")
    private String token;

    @Resource
    private ILoginService loginService;
    
    @Resource
    private WeixinBindService weixinBindService;
    
    /**
     * 绑定消息正则表达式：匹配"绑定 xxxx"格式
     */
    private static final Pattern BIND_PATTERN = Pattern.compile("绑定\\s*(\\w+)");
    private static final Pattern BIND_PATTERN_CN = Pattern.compile("绑定码[：:]?\\s*(\\w+)");
    private static final Pattern BIND_PATTERN_EN = Pattern.compile("bind\\s*(\\w+)", Pattern.CASE_INSENSITIVE);

    @GetMapping(value = "receive", produces = "text/plain;charset=utf-8")
    public String validate(@RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            log.info("微信公众号验签信息开始 [{}, {}, {}, {}]", signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = SignatureUtil.check(token, signature, timestamp, nonce);
            log.info("微信公众号验签信息完成 check：{}", check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            log.error("微信公众号验签信息失败 [{}, {}, {}, {}]", signature, timestamp, nonce, echostr, e);
            return null;
        }
    }

    @PostMapping(value = "receive", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        try {
            log.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
            // 消息转换
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            
            log.info("解析后的消息对象: MsgType={}, Event={}, EventKey={}, Ticket={}, Content={}", 
                message.getMsgType(), message.getEvent(), message.getEventKey(), 
                message.getTicket(), message.getContent());

            if ("event".equals(message.getMsgType()) && "SCAN".equals(message.getEvent())) {
                log.info("处理 SCAN 事件 - Ticket={}, EventKey={}", message.getTicket(), message.getEventKey());
                try {
                    loginService.handleWechatScanLogin(message.getTicket(), openid);
                    return buildMessageTextEntity(openid, "登录成功");
                } catch (Exception e) {
                    log.error("微信扫码登录处理失败: ticket={}, openid={}", message.getTicket(), openid, e);
                    return buildMessageTextEntity(openid, "登录失败，请重试");
                }
            }
            
            // 处理绑定消息
            if ("text".equals(message.getMsgType())) {
                String content = message.getContent();
                String extractedTicket = extractBindTicket(content);
                if (extractedTicket != null) {
                    String status = weixinBindService.getBindStatusRaw(extractedTicket);
                    if (status != null) {
                        weixinBindService.updateBindStatus(extractedTicket, openid);
                        return buildMessageTextEntity(openid, "绑定成功！您的微信账号已与注册码 " + extractedTicket + " 关联");
                    } else {
                        return buildMessageTextEntity(openid, "无效的绑定码，请在注册页面获取有效的绑定码后重试");
                    }
                }
            }

            return buildMessageTextEntity(openid, "你好，" + message.getContent());
        } catch (Exception e) {
            log.error("接收微信公众号信息请求{}失败 {}", openid, requestBody, e);
            return "";
        }
    }
    
    /**
     * 从消息内容中提取绑定码
     * 
     * @param content 消息内容
     * @return 绑定码，如果未找到返回 null
     */
    private String extractBindTicket(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        
        Matcher matcher = BIND_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        matcher = BIND_PATTERN_CN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        matcher = BIND_PATTERN_EN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    private String buildMessageTextEntity(String openid, String content) {
        MessageTextEntity res = new MessageTextEntity();
        // 公众号分配的ID
        res.setFromUserName(originalid);
        res.setToUserName(openid);
        res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
        res.setMsgType("text");
        res.setContent(content);
        return XmlUtil.beanToXml(res);
    }

}
