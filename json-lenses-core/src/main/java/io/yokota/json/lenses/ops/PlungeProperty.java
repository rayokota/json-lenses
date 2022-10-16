package io.yokota.json.lenses.ops;

import java.util.Objects;

public class PlungeProperty extends LensOp {
    private final String name;
    private final String host;

    public PlungeProperty(String name, String host) {
        this.name = name;
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlungeProperty that = (PlungeProperty) o;
        return Objects.equals(name, that.name) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, host);
    }
}
