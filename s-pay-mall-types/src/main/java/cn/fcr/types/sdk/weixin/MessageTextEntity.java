package cn.fcr.types.sdk.weixin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 微信消息 XML 映射实体，对应微信公众平台推送的各类消息与事件
 *
 * @author 傅崇睿
 */
@JacksonXmlRootElement(localName = "xml")
public class MessageTextEntity {

    /** 接收方账号（公众号） */
    @JacksonXmlProperty(localName = "ToUserName")
    private String toUserName;

    /** 发送方账号（用户 OpenID） */
    @JacksonXmlProperty(localName = "FromUserName")
    private String fromUserName;

    /** 消息创建时间 */
    @JacksonXmlProperty(localName = "CreateTime")
    private String createTime;

    /** 消息类型（text / event 等） */
    @JacksonXmlProperty(localName = "MsgType")
    private String msgType;

    /** 事件类型（subscribe / SCAN 等） */
    @JacksonXmlProperty(localName = "Event")
    private String event;

    /** 事件 KEY（如 qrscene_ 前缀的场景值） */
    @JacksonXmlProperty(localName = "EventKey")
    private String eventKey;

    /** 消息 ID */
    @JacksonXmlProperty(localName = "MsgID")
    private String msgId;

    /** 事件状态 */
    @JacksonXmlProperty(localName = "Status")
    private String status;

    /** 票据（用于获取二维码 ticket） */
    @JacksonXmlProperty(localName = "Ticket")
    private String ticket;

    /** 文本消息内容 */
    @JacksonXmlProperty(localName = "Content")
    private String content;

    // Getters and Setters
    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}