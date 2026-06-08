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

public class XmlUtil {

    private static final XmlMapper XML_MAPPER;

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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

    public static <T> T xmlToBean(String resultXml, Class<T> clazz) {
        try {
            return XML_MAPPER.readValue(resultXml, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}