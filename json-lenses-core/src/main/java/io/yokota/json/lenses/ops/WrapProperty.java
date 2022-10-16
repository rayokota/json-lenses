package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WrapProperty extends LensOp {
    private final String name;

    public WrapProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String op = patchOp.get("op").textValue();
        String path = patchOp.get("path").textValue();
        Object value = patchOp.get("value").textValue();
        Pattern p = Pattern.compile("^/(" + name + ")(.*)");
        Matcher m = p.matcher(path);
        if (m.find()) {
            path = "/" + m.group(1) + "/0" + m.group(2);
            if ((op.equals("add") || op.equals("replace)"))
                && value == null && m.group(2).equals("")) {
                ObjectNode copy = JsonNodeFactory.instance.objectNode();
                copy.put("op", "remove");
                copy.put("path", path);
                return copy;
            }
            ObjectNode copy = patchOp.deepCopy();
            copy.put("path", path);
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new HeadProperty(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WrapProperty that = (WrapProperty) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
