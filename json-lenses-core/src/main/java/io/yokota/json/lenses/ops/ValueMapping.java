package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class ValueMapping {
    private final Map<Object, Object> forward;
    private final Map<Object, Object> reverse;

    @JsonCreator
    public ValueMapping(@JsonProperty("forward") Map<Object, Object> forward,
                        @JsonProperty("reverse") Map<Object, Object> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    @JsonProperty("forward")
    public Map<Object, Object> getForward() {
        return forward;
    }

    @JsonProperty("reverse")
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
