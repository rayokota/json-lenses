package io.yokota.json.lenses.ops;

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
