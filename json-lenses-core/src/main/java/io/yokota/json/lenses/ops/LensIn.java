package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.Context;
import io.yokota.json.lenses.JsonLenses;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LensIn extends LensOp {
    private final String name;
    private final List<LensOp> lens;

    @JsonCreator
    public LensIn(@JsonProperty("name") String name,
                  @JsonProperty("lens") List<LensOp> lens) {
        this.name = name;
        this.lens = lens;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("lens")
    public List<LensOp> getLens() {
        return lens;
    }

    @Override
    public void apply(Context ctx) {
        lens.forEach(l -> l.apply(ctx.getSubcontext(name)));
    }

    @Override
    public JsonNode apply(JsonNode patchOp) {
        String path = patchOp.get("path").textValue();
        // Run the inner body in a context where the path has been narrowed down...
        Pattern p = Pattern.compile("^/" + name);
        Matcher m = p.matcher(path);
        if (m.find()) {
            ObjectNode copy = patchOp.deepCopy();
            copy.put("path", path.replaceFirst("^/" + name, ""));
            JsonNode childPatch = JsonLenses.applyLensToPatchOp(lens, copy);
            if (childPatch != null) {
                copy = childPatch.deepCopy();
                copy.put("path", "/" + name + childPatch.get("path").textValue());
                return copy;
            } else {
                return null;
            }
        }
        return patchOp;
    }

    @Override
    public LensOp reverse() {
        return new LensIn(name, JsonLenses.reverse(lens));
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
