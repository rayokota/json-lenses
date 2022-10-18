package io.yokota.json.lenses.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.List;

public class Convert {
    private Convert() {
        /* singleton */
    }

    public static Object jsonNodeToPrimitive(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else {
            throw new IllegalArgumentException(
                "Unsupported node type " + node.getClass().getName());
        }
    }

    public static JsonNode valueToJsonNode(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof Byte) {
            return JsonNodeFactory.instance.numberNode(((Byte) value).intValue());
        } else if (value instanceof Short) {
            return JsonNodeFactory.instance.numberNode(((Short) value).intValue());
        } else if (value instanceof Integer) {
            return JsonNodeFactory.instance.numberNode(((Integer) value));
        } else if (value instanceof Long) {
            return JsonNodeFactory.instance.numberNode(((Long) value));
        } else if (value instanceof Float) {
            return JsonNodeFactory.instance.numberNode(((Float) value));
        } else if (value instanceof Double) {
            return JsonNodeFactory.instance.numberNode(((Double) value));
        } else if (value instanceof Boolean) {
            return JsonNodeFactory.instance.booleanNode(((Boolean) value));
        } else if (value instanceof String) {
            return JsonNodeFactory.instance.textNode(((String) value));
        } else if (value.getClass().isArray() || value instanceof List) {
            // assume array is empty
            return JsonNodeFactory.instance.arrayNode();
        } else {
            // assume object is empty
            return JsonNodeFactory.instance.objectNode();
        }
    }
}
