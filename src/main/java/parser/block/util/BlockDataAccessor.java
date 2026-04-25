package parser.block.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import parser.util.NumberParser;

import java.util.*;

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
        return NumberParser.parseDoubleOrZero(getString(key));
    }

    public int getInt(String key) {
        return NumberParser.parseIntOrZero(getString(key));
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

    /** Возвращает вложенный объект как BlockDataAccessor (или null) */
    public BlockDataAccessor getAccessor(String key) {
        if (data instanceof JsonNode json) {
            JsonNode child = json.get(key);
            return child != null && !child.isNull() ? new BlockDataAccessor(child) : null;
        }
        if (data instanceof Map<?, ?> map) {
            Object child = map.get(key);
            return child != null ? new BlockDataAccessor(child) : null;
        }
        if (data instanceof Node xml) {
            Node child = findXmlChild(xml, key);
            return child != null ? new BlockDataAccessor(child) : null;
        }
        return null;
    }

    /** Возвращает список вложенных объектов (массив/дочерние элементы) */
    public List<BlockDataAccessor> getAccessorList(String key) {
        List<BlockDataAccessor> result = new ArrayList<>();
        if (data instanceof JsonNode json) {
            JsonNode arr = json.get(key);
            if (arr != null && arr.isArray()) {
                arr.forEach(item -> result.add(new BlockDataAccessor(item)));
            }
        } else if (data instanceof Map<?, ?> map) {
            Object v = map.get(key);
            if (v instanceof List<?> list) {
                list.forEach(item -> result.add(new BlockDataAccessor(item)));
            }
        } else if (data instanceof Node xml) {
            Node container = findXmlChild(xml, key);
            if (container != null) {
                NodeList children = container.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        result.add(new BlockDataAccessor(children.item(i)));
                    }
                }
            }
        }
        return result;
    }

    /** Возвращает имена полей верхнего уровня */
    public Set<String> getFieldNames() {
        Set<String> names = new LinkedHashSet<>();
        if (data instanceof JsonNode json) {
            json.fieldNames().forEachRemaining(names::add);
        } else if (data instanceof Map<?, ?> map) {
            map.keySet().forEach(k -> names.add(k.toString()));
        } else if (data instanceof Node xml) {
            NodeList children = xml.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    names.add(child.getNodeName());
                }
            }
        }
        return names;
    }

    /** Возвращает «сырой» дочерний объект для передачи в BlockParserChain */
    public Object getRawChild(String key) {
        if (data instanceof JsonNode json) {
            return json.get(key);
        }
        if (data instanceof Map<?, ?> map) {
            return map.get(key);
        }
        if (data instanceof Node xml) {
            return findXmlChild(xml, key);
        }
        return null;
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