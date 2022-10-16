package io.yokota.json.lenses.ops;

import java.util.Objects;

public class ConvertValue extends LensOp {
    private final String name;
    private final ValueMapping mapping;

    public ConvertValue(String name, ValueMapping mapping) {
        this.name = name;
        this.mapping = mapping;
    }

    public String getName() {
        return name;
    }

    public ValueMapping getMapping() {
        return mapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConvertValue that = (ConvertValue) o;
        return Objects.equals(name, that.name) && Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, mapping);
    }
}
