package io.yokota.json.lenses.ops;

import java.util.List;
import java.util.Objects;

public class LensIn extends LensOp {
    private final String name;
    private final List<LensOp> lens;

    public LensIn(String name, List<LensOp> lens) {
        this.name = name;
        this.lens = lens;
    }

    public String getName() {
        return name;
    }

    public List<LensOp> getLens() {
        return lens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LensIn lensIn = (LensIn) o;
        return Objects.equals(name, lensIn.name) && Objects.equals(lens, lensIn.lens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, lens);
    }
}
