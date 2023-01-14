package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.yokota.json.lenses.Context;

import java.util.Objects;

public class AddProperty extends LensOp {
    private final String name;
    private final Object defaultValue;

    @JsonCreator
    public AddProperty(@JsonProperty("name") String name,
                       @JsonProperty("defaultValue") Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("defaultValue")
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void apply(Context ctx) {
        ctx.getSubcontext(name).setDefaultValue(defaultValue);
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
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
