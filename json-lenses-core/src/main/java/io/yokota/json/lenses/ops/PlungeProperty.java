package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlungeProperty extends LensOp {
    private final String name;
    private final String host;

    public PlungeProperty(String name, String host) {
        this.name = name;
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
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
        if (pathElements.length >= 1 && pathElements[0].equals(name)) {
            pathElements[0] = "";
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
        return new HoistProperty(name, host);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlungeProperty that = (PlungeProperty) o;
        return Objects.equals(name, that.name) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, host);
    }
}
