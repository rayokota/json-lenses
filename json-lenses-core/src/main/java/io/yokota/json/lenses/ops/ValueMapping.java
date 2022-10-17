package io.yokota.json.lenses.ops;

import java.util.Map;
import java.util.Objects;

public class ValueMapping {
    private final Map<Object, Object> forward;
    private final Map<Object, Object> reverse;

    public ValueMapping(Map<Object, Object> forward, Map<Object, Object> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public Map<Object, Object> getForward() {
        return forward;
    }

    public Map<Object, Object> getReverse() {
        return reverse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueMapping that = (ValueMapping) o;
        return Objects.equals(forward, that.forward) && Objects.equals(reverse, that.reverse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forward, reverse);
    }
}
