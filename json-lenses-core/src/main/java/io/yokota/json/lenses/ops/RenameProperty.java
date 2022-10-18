package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;

import java.util.Objects;

public class RenameProperty extends LensOp {
    private final String source;
    private final String target;

    public RenameProperty(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public JsonNode apply(Context ctx, JsonNode patchOp) {
        Object defaultValue = ctx.getDefaultValue(source);
        if (defaultValue != null) {
            ctx.setDefaultValue(target, defaultValue);
        }

        String op = patchOp.get("op").textValue();
        String path = patchOp.get("path").textValue();
        String[] pathElements = path.split("/");
        // TODO: what about other JSON patch op types?
        // (consider other parts of JSON patch: move / copy / test / remove ?)
        if ((op.equals("replace") || op.equals("add"))
            && pathElements.length > 1
            && pathElements[1].equals(source)) {
            path = path.replace(source, target);
            ObjectNode copy = patchOp.deepCopy();
            copy.put("path", path);
            return copy;
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new RenameProperty(target, source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RenameProperty that = (RenameProperty) o;
        return Objects.equals(source, that.source) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), source, target);
    }
}
