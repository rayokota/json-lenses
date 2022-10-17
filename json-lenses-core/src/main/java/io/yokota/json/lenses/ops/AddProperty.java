package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import io.yokota.json.lenses.Context;

import java.util.Objects;

public class AddProperty extends LensOp {
    private final String name;
    private final Object defaultValue;

    public AddProperty(String name, Object defaultValue) {
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
    public JsonNode apply(Context ctx, JsonNode patchOp) {
        ctx.setDefaultValue(name, defaultValue);

        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new RemoveProperty(name, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddProperty that = (AddProperty) o;
        return Objects.equals(name, that.name) && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, defaultValue);
    }
}
