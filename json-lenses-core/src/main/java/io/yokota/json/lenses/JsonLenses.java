package io.yokota.json.lenses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.utils.Convert;
import io.yokota.json.lenses.utils.Jackson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonLenses {

    private static final JsonNode EMPTY_DOC = emptyDoc();

    public static JsonNode applyLensToDoc(
        List<LensOp> lens, JsonNode inputDoc, JsonNode targetDoc) {
        ArrayNode patchForOriginalDoc = (ArrayNode) JsonDiff.asJson(EMPTY_DOC, inputDoc);

        Context ctx = new Context();
        ArrayNode outputPatch = applyLensToPatch(ctx, lens, patchForOriginalDoc);
        JsonNode base = defaultObjectForContext(ctx);
        if (targetDoc != null) {
            Jackson.merge(base, targetDoc);
        }

        return JsonPatch.apply(outputPatch, base);
    }

    private static JsonNode defaultObjectForContext(Context ctx) {
        // By setting the root to empty object,
        // we kick off a recursive process that fills in the entire thing
        ObjectNode patch = JsonNodeFactory.instance.objectNode();
        patch.put("op", "add");
        patch.put("path", "");
        patch.set("value", JsonNodeFactory.instance.objectNode());

        List<JsonNode> defaultsPatch = addDefaultValues(ctx, Collections.singletonList(patch));
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.addAll(defaultsPatch);

        return JsonPatch.apply(patches, emptyDoc());
    }

    public static ArrayNode applyLensToPatch(List<LensOp> lens, ArrayNode patch) {
        return applyLensToPatch(new Context(), lens, patch);
    }

    private static ArrayNode applyLensToPatch(Context ctx, List<LensOp> lens, ArrayNode patch) {
        Iterable<JsonNode> iterable = patch::elements;
        List<JsonNode> lensedPatch = StreamSupport.stream(iterable.spliterator(), false)
            .flatMap(op -> expandPatch(op).stream())
            .map(patchOp -> applyLensToPatchOp(lens, patchOp))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        updateDefaultValues(ctx, lens);
        List<JsonNode> lensedPatchWithDefaults = addDefaultValues(ctx, lensedPatch);

        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        result.addAll(lensedPatchWithDefaults);
        return result;
    }

    public static JsonNode applyLensToPatchOp(List<LensOp> lens, JsonNode patchOp) {
        return lens.stream()
            .reduce(patchOp,
                (prevPatch, lensOp) -> applyLensOp(lensOp, prevPatch),
                (prevPatch, currPatch) -> {
                    throw new UnsupportedOperationException(); // unused combiner
                }
            );
    }

    private static JsonNode applyLensOp(LensOp lensOp, JsonNode patchOp) {
        if (patchOp == null) {
            return null;
        }
        return lensOp.apply(patchOp);
    }

    protected static List<JsonNode> expandPatch(JsonNode patch) {
        String op = patch.get("op").textValue();
        if (!op.equals("add") && !op.equals("replace")) {
            return Collections.singletonList(patch);
        }

        JsonNode value = patch.get("value");
        if (value.isObject() || value.isArray()) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.set("op", patch.get("op"));
            node.set("path", patch.get("path"));
            node.set("value", value.isArray()
                ? JsonNodeFactory.instance.arrayNode()
                : JsonNodeFactory.instance.objectNode());

            return flatten(Stream.concat(
                    Stream.of(node),
                    entries(value)
                        .map(e -> {
                            ObjectNode n = JsonNodeFactory.instance.objectNode();
                            n.set("op", patch.get("op"));
                            n.set("path", JsonNodeFactory.instance.textNode(
                                patch.get("path").textValue() + "/" + e.getKey()));
                            n.set("value", e.getValue());
                            return expandPatch(n);
                        })));
        }

        return Collections.singletonList(patch);
    }

    private static Stream<Map.Entry<Object, JsonNode>> entries(JsonNode value) {
        if (value.isObject()) {
            Iterable<Map.Entry<String, JsonNode>> iterable = value::fields;
            return StreamSupport.stream(iterable.spliterator(), false)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
        } else if (value.isArray()) {
            List<Map.Entry<Object, JsonNode>> elements = new ArrayList<>();
            int i = 0;
            for (Iterator<JsonNode> it = value.elements(); it.hasNext(); i++) {
                elements.add(new AbstractMap.SimpleEntry<>(i, it.next()));
            }
            return elements.stream();
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass().getName());
        }
    }

    private static void updateDefaultValues(Context ctx, List<LensOp> lens) {
        lens.forEach(l -> l.apply(ctx));
    }

    protected static List<JsonNode> addDefaultValues(Context ctx, List<JsonNode> patch) {
        return flatten(patch.stream()
            .map(patchOp -> {
                String op = patchOp.get("op").textValue();
                String path = patchOp.get("path").textValue();
                JsonNode value = patchOp.get("value");
                boolean isMakeMap = (op.equals("add") || op.equals("replace"))
                    && value.isObject()
                    && !value.fields().hasNext();

                if (!isMakeMap) {
                    return patchOp;
                }

                Context subctx = ctx.getSubcontextForPath(path);

                return flatten(Stream.concat(
                        Stream.of(patchOp),
                        subctx.getSubcontexts().entrySet().stream()
                            .map(e -> {
                                String subpath = path + "/" + e.getKey();
                                Object defaultVal = e.getValue().getDefaultValue();
                                if (defaultVal != null) {
                                    ObjectNode copy = patchOp.deepCopy();
                                    copy.put("path", subpath);
                                    copy.replace("value", Convert.valueToJsonNode(defaultVal));
                                    return addDefaultValues(ctx, Collections.singletonList(copy));
                                }
                                return Collections.emptyList();
                            })
                    ));
            }));
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> flatten(Stream<?> stream) {
        List<Object> result = stream
            .flatMap(child -> {
                if (child == null) {
                    return Stream.empty();
                } else if (child instanceof Collection) {
                    return flatten(((Collection<?>) child).stream()).stream();
                } else {
                    return Stream.of((T) child);
                }
            })
            .collect(Collectors.toList());
        return (List<T>) result;
    }

    public static JsonNode emptyDoc() {
        return JsonNodeFactory.instance.objectNode();
    }

    public static List<LensOp> reverse(List<LensOp> lens) {
        return lens.stream()
            .map(LensOp::reverse)
            .collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode source = om.readTree("{\"a\":  1, \"b\":  2}");
        JsonNode target = om.readTree("{\"c\":  1, \"d\":  2}");
        JsonNode patch = JsonDiff.asJson(source, target);
        System.out.println(patch);
    }
}
