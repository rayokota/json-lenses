package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;
import io.yokota.json.lenses.utils.Convert;

import java.util.HashMap;
import java.util.Objects;

public class ConvertValue extends LensOp {
    private final String name;
    private final ValueMapping mapping;

    @JsonCreator
    public ConvertValue(@JsonProperty("name") String name,
                        @JsonProperty("mapping") ValueMapping mapping) {
        this.name = name;
        this.mapping = mapping;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("mapping")
    public ValueMapping getMapping() {
        return mapping;
    }

    @Override
    public void apply(Context ctx) {
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
        Object value = Convert.jsonNodeToPrimitive(patchOp.get("value"));

        // TODO: should we add in support for fallback/default conversions
        if (!mapping.getForward().containsKey(value)) {
            throw new IllegalArgumentException("No mapping for value: " + value);
        }

        ObjectNode copy = patchOp.deepCopy();
        copy.set("value", Convert.valueToJsonNode(mapping.getForward().get(value)));
        return copy;
    }

    @Override
    public LensOp reverse() {
        return new ConvertValue(name, new ValueMapping(
            new HashMap<>(mapping.getReverse()),
            new HashMap<>(mapping.getForward()))
        );
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
