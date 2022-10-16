package io.yokota.json.lenses.ops;

import java.util.Map;
import java.util.Objects;

public class ValueMapping {
    private final Map<String, String> forward;
    private final Map<String, String> reverse;

    public ValueMapping(Map<String, String> forward, Map<String, String> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public Map<String, String> getForward() {
        return forward;
    }

    public Map<String, String> getReverse() {
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
