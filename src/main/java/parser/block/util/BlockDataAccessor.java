package parser.block.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockDataAccessor {

    private final Object data;

    public BlockDataAccessor(Object data) {
        this.data = data;
    }

    public String getString(String key) {
        if (data instanceof JsonNode json) {
            JsonNode node = json.get(key);
            return node != null && !node.isNull() ? node.asText() : null;
        }
        if (data instanceof Map<?, ?> map) {
            Object v = map.get(key);
            return v != null ? v.toString() : null;
        }
        if (data instanceof Node xml) {
            return getXmlChildText(xml, key);
        }
        return null;
    }

    public double getDouble(String key) {
        String v = getString(key);
        if (v == null || v.isBlank()) return 0.0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0.0; }
    }

    public int getInt(String key) {
        String v = getString(key);
        if (v == null || v.isBlank()) return 0;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    public boolean getBoolean(String key) {
        String v = getString(key);
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    public List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        if (data instanceof JsonNode json) {
            JsonNode arr = json.get(key);
            if (arr != null && arr.isArray()) {
                arr.forEach(n -> result.add(n.asText()));
            }
        } else if (data instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v instanceof List<?> list) {
                list.forEach(item -> result.add(item.toString()));
            }
        } else if (data instanceof Node xml) {
            Node child = findXmlChild(xml, key);
            if (child != null) {
                NodeList items = child.getChildNodes();
                for (int i = 0; i < items.getLength(); i++) {
                    Node item = items.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        result.add(item.getTextContent().trim());
                    }
                }
            }
        }
        return result;
    }

    private String getXmlChildText(Node parent, String tag) {
        Node child = findXmlChild(parent, tag);
        return child != null ? child.getTextContent().trim() : null;
    }

    private Node findXmlChild(Node parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }
}