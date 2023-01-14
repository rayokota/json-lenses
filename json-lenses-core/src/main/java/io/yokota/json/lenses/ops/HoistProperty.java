package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;

import java.util.Objects;

public class HoistProperty extends LensOp {
    private final String host;
    private final String name;

    @JsonCreator
    public HoistProperty(@JsonProperty("host") String host,
                         @JsonProperty("name") String name) {
        this.host = host;
        this.name = name;
    }

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    public void apply(Context ctx) {
        Context nameCtx = ctx.getSubcontext(host).removeSubcontext(name);
        if (nameCtx != null) {
            ctx.setSubcontext(name, nameCtx);
        }
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String path = patchOp.get("path").textValue();
        // leading slash needs trimming
        String[] pathElements = path.substring(1).split("/");
        if (pathElements.length > 1 && pathElements[0].equals(host) && pathElements[1].equals(name)) {
            pathElements[0] = "";
            path = String.join("/", pathElements);
            ObjectNode copy = patchOp.deepCopy();
            copy.put("path", path);
            return copy;
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new PlungeProperty(host, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HoistProperty that = (HoistProperty) o;
        return Objects.equals(host, that.host) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, name);
    }
}
