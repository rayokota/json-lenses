package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class RemoveProperty extends LensOp {
    private final String name;
    private final Object defaultValue;

    public RemoveProperty(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String path = patchOp.get("path").textValue();
        String[] pathElements = path.split("/");
        boolean match = pathElements.length >= 2 && pathElements[1].equals(name);
        if (match) {
            return null;
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new AddProperty(name, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveProperty that = (RemoveProperty) o;
        return Objects.equals(name, that.name) && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, defaultValue);
    }
}
