package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class RemoveProperty extends LensOp {
    private final String name;

    public RemoveProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String op = patchOp.get("op").textValue();
        String path = patchOp.get("path").textValue();
        String[] pathElements = path.split("/");
        boolean match = pathElements.length >= 2 && pathElements[1].equals(name);
        if (match) {
            return null;
        }
        return patchOp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveProperty that = (RemoveProperty) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
