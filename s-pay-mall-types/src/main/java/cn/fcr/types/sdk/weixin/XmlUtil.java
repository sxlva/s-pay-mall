package cn.fcr.types.sdk.weixin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.*;

/**
 * 微信消息 XML 格式与 Map、Java Bean 之间的相互转换工具
 *
 * @author 傅崇睿
 */
public class XmlUtil {

    /** Jackson XML 映射器（忽略未知属性） */
    private static final XmlMapper XML_MAPPER;

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 将 HTTP 请求中的 XML 报文解析为 Map
     *
     * @param request HTTP 请求
     * @return XML 元素名到文本值的映射
     * @throws Exception 解析异常
     */
    public static Map<String, String> xmlToMap(HttpServletRequest request) throws Exception {
        try (InputStream inputStream = request.getInputStream()) {
            Map<String, String> map = new HashMap<>();
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();
            List<Element> elementList = root.elements();
            for (Element e : elementList)
                map.put(e.getName(), e.getText());
            return map;
        }
    }

    /**
     * 将 Map 转换为微信 XML 字符串
     *
     * @param map 待转换的 Map
     * @return XML 字符串，失败返回 null
     */
    static String mapToXML(Map map) {
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        mapToXML2(map, sb);
        sb.append("</xml>");
        try {
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 递归将 Map 转换为 XML（内部辅助方法）
     *
     * @param map 待转换的 Map
     * @param sb  拼接目标 StringBuffer
     */
    private static void mapToXML2(Map map, StringBuffer sb) {
        Set set = map.keySet();
        for (Object o : set) {
            String key = (String) o;
            Object value = map.get(key);
            if (null == value)
                value = "";
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                ArrayList list = (ArrayList) map.get(key);
                sb.append("<").append(key).append(">");
                for (Object o1 : list) {
                    HashMap hm = (HashMap) o1;
                    mapToXML2(hm, sb);
                }
                sb.append("</").append(key).append(">");

            } else {
                if (value instanceof HashMap) {
                    sb.append("<").append(key).append(">");
                    mapToXML2((HashMap) value, sb);
                    sb.append("</").append(key).append(">");
                } else {
                    sb.append("<").append(key).append("><![CDATA[").append(value).append("]]></").append(key).append(">");
                }

            }

        }
    }

    /**
     * 将 Java Bean 序列化为微信 XML 字符串
     *
     * @param object 待序列化的 Bean
     * @return 格式化后的微信 XML 字符串，失败返回 null
     */
    public static String beanToXml(Object object) {
        try {
            String xml = XML_MAPPER.writeValueAsString(object);
            if (!StringUtils.isEmpty(xml)) {
                return formatWeixinXml(xml);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 格式化 XML 为微信要求的格式（添加 CDATA 包裹非数字文本）
     *
     * @param xml 原始 XML 字符串
     * @return 格式化后的微信 XML 字符串
     */
    private static String formatWeixinXml(String xml) {
        xml = xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        xml = xml.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");

        StringBuilder result = new StringBuilder();
        result.append("<xml>");

        int start = xml.indexOf("<xml>") + 5;
        int end = xml.lastIndexOf("</xml>");

        if (start < end) {
            String content = xml.substring(start, end);
            String[] elements = content.split("(?=</)");

            for (String element : elements) {
                if (StringUtils.isBlank(element)) continue;

                int tagStart = element.indexOf("<");
                int tagEnd = element.indexOf(">");
                if (tagStart >= 0 && tagEnd > tagStart) {
                    String tagName = element.substring(tagStart + 1, tagEnd);
                    String text = element.substring(tagEnd + 1).trim();

                    result.append("<").append(tagName).append(">");
                    if (!StringUtils.isNumeric(text)) {
                        result.append("<![CDATA[").append(text).append("]]>");
                    } else {
                        result.append(text);
                    }
                    result.append("</").append(tagName).append(">");
                }
            }
        }

        result.append("</xml>");
        return result.toString();
    }

    /**
     * 将微信 XML 字符串反序列化为 Java Bean
     *
     * @param resultXml XML 字符串
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 反序列化后的 Bean，失败返回 null
     */
    public static <T> T xmlToBean(String resultXml, Class<T> clazz) {
        try {
            return XML_MAPPER.readValue(resultXml, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}