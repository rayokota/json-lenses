package io.yokota.json.lenses.ops;

import java.util.List;
import java.util.Objects;

public class LensMap extends LensOp {
    private final List<LensOp> lens;

    public LensMap(List<LensOp> lens) {
        this.lens = lens;
    }

    public List<LensOp> getLens() {
        return lens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LensMap lensIn = (LensMap) o;
        return Objects.equals(lens, lensIn.lens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lens);
    }
}
