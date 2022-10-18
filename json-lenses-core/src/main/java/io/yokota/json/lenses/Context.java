package io.yokota.json.lenses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Context {

    private Object defaultValue;
    private final Map<String, Context> subcontexts;

    public Context() {
        this(null, new HashMap<>());
    }

    public Context(Object defaultValue, Map<String, Context> subcontexts) {
        this.defaultValue = defaultValue;
        this.subcontexts = subcontexts;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object removeDefaultValue() {
        Object result = defaultValue;
        defaultValue = null;
        return result;
    }

    public Context getSubcontext(String name) {
        return subcontexts.computeIfAbsent(name, k -> new Context());
    }

    public Map<String, Context> getSubcontexts() {
        return subcontexts;
    }

    public void setSubcontext(String name, Context subctx) {
        subcontexts.put(name, subctx);
    }

    public Context removeSubcontext(String name) {
        return subcontexts.remove(name);
    }
    public Context getSubcontextForPath(String path) {
        return Arrays.stream(path.split("/"))
            .skip(1)
            .reduce(this,
                (prevCtx, pathSegment) -> {
                    if (pathSegment.matches("[0-9]+")) {
                        return prevCtx;
                    } else {
                        return prevCtx.getSubcontext(pathSegment);
                    }
                },
                (prevCtx, currCtx) -> {
                    throw new UnsupportedOperationException(); // unused combiner
                }
            );
    }
}
