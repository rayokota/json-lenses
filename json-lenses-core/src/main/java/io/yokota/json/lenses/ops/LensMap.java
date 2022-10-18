package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;
import io.yokota.json.lenses.JsonLenses;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LensMap extends LensOp {
    private final List<LensOp> lens;

    public LensMap(List<LensOp> lens) {
        this.lens = lens;
    }

    public List<LensOp> getLens() {
        return lens;
    }

    @Override
    public void apply(Context ctx) {
        lens.forEach(l -> l.apply(ctx));
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String path = patchOp.get("path").textValue();
        Pattern p = Pattern.compile("/([0-9]+)/");
        Matcher m = p.matcher(path);
        if (!m.find()) {
            return patchOp;
        }
        String arrayIndex = m.group(1);
        ObjectNode copy = patchOp.deepCopy();
        copy.put("path", path.replaceFirst("/[0-9]+/", "/"));
        JsonNode itemPatch = JsonLenses.applyLensToPatchOp(lens, copy);
        if (itemPatch != null) {
            copy = itemPatch.deepCopy();
            copy.put("path", "/" + arrayIndex + itemPatch.get("path").textValue());
            return copy;
        }
        return null;
    }

    @Override
    public LensOp reverse() {
        return new LensMap(JsonLenses.reverse(lens));
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
