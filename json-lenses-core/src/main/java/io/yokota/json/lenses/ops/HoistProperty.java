package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

public class HoistProperty extends LensOp {
    private final String name;
    private final String host;

    public HoistProperty(String name, String host) {
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
    public JsonNode apply(JsonNode patchOp) {
        String path = patchOp.get("path").textValue();
        // leading slash needs trimming
        String[] pathElements = path.substring(1).split("/");
        if (pathElements.length >= 2 && pathElements[0].equals(host) && pathElements[1].equals(name)) {
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
        return new PlungeProperty(name, host);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HoistProperty that = (HoistProperty) o;
        return Objects.equals(name, that.name) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, host);
    }
}
