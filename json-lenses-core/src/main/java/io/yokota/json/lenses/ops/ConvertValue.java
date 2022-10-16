package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

public class ConvertValue extends LensOp {
    private final String name;
    private final ValueMapping mapping;

    public ConvertValue(String name, ValueMapping mapping) {
        this.name = name;
        this.mapping = mapping;
    }

    public String getName() {
        return name;
    }

    public ValueMapping getMapping() {
        return mapping;
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String op = patchOp.get("op").textValue();
        String path = patchOp.get("path").textValue();
        if (!op.equals("add") && !op.equals("replace")) {
            return patchOp;
        }
        if (!path.equals("/" + name)) {
            return patchOp;
        }
        // TODO check
        String stringifiedValue = patchOp.get("value").toString();

        // TODO: should we add in support for fallback/default conversions
        if (!mapping.getForward().containsKey(stringifiedValue)) {
            throw new IllegalArgumentException("No mapping for value: " + stringifiedValue);
        }

        ObjectNode copy = patchOp.deepCopy();
        copy.put("value", mapping.getForward().get(stringifiedValue));
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConvertValue that = (ConvertValue) o;
        return Objects.equals(name, that.name) && Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, mapping);
    }
}
