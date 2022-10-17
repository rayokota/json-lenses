package io.yokota.json.lenses;

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

    public Object getDefaultValue(String name) {
        return getSubcontext(name).getDefaultValue();
    }

    public void setDefaultValue(String name, Object value) {
        getSubcontext(name).setDefaultValue(value);
    }

    public Object removeDefaultValue(String name) {
        return getSubcontext(name).removeDefaultValue();
    }

    public Context getSubcontext(String name) {
        return subcontexts.computeIfAbsent(name, k -> new Context());
    }

    public Map<String, Context> getSubcontexts() {
        return subcontexts;
    }
}
