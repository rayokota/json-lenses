package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlungeProperty extends LensOp {
    private final String host;
    private final String name;

    public PlungeProperty(String host, String name) {
        this.host = host;
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonNode apply(Context ctx, JsonNode patchOp) {
        Context subctx = ctx.getSubcontext(host);
        Object defaultValue = subctx.removeDefaultValue(name);
        if (defaultValue != null) {
            ctx.setDefaultValue(name, defaultValue);
        }

        String path = patchOp.get("path").textValue();
        // leading slash needs trimming
        String[] pathElements = path.substring(1).split("/");
        if (pathElements.length > 0 && pathElements[0].equals(name)) {
            List<String> newPathElements = new ArrayList<>();
            newPathElements.add("");
            newPathElements.add(host);
            newPathElements.addAll(Arrays.asList(pathElements));
            path = String.join("/", newPathElements);
            ObjectNode copy = patchOp.deepCopy();
            copy.put("path", path);
            return copy;
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new HoistProperty(host, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlungeProperty that = (PlungeProperty) o;
        return Objects.equals(host, that.host) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, name);
    }
}
