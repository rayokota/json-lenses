package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadProperty extends LensOp {
    private final String name;

    @JsonCreator
    public HeadProperty(@JsonProperty("name") String name) {
        this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    public void apply(Context ctx) {
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String op = patchOp.get("op").textValue();
        String path = patchOp.get("path").textValue();
        String[] pathElements = path.split("/");
        // return early if we're not handling a write to the array handled by this lens
        boolean arrayMatch = pathElements.length > 1 && pathElements[1].equals(name);
        if (!arrayMatch) {
            return patchOp;
        }
        // We only care about writes to the head element, nothing else matters
        Pattern p = Pattern.compile("^/" + name + "/0(.*)");
        Matcher m = p.matcher(path);
        if (!m.find()) {
            return null;
        }

        if (op.equals("add") || op.equals("replace")) {
            // If the write is to the first array element, write to the scalar
            ObjectNode copy = JsonNodeFactory.instance.objectNode();
            copy.put("op", op);
            copy.put("path", "/" + name + (m.group(1) != null ? m.group(1) : ""));
            copy.replace("value", patchOp.get("value"));
            return copy;
        }

        if (op.equals("remove")) {
            if (m.group(1).equals("")) {
                ObjectNode copy = JsonNodeFactory.instance.objectNode();
                copy.put("op", "replace");
                copy.put("path", "/" + name + (m.group(1) != null ? m.group(1) : ""));
                copy.replace("value", null);
                return copy;
            } else {
                ObjectNode copy = patchOp.deepCopy();
                copy.put("path", "/" + name + (m.group(1) != null ? m.group(1) : ""));
                return copy;
            }
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new WrapProperty(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HeadProperty that = (HeadProperty) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
