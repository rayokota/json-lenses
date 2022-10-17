package io.yokota.json.lenses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.utils.Jackson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

        JsonNode base = targetDoc != null ? targetDoc : emptyDoc();
        JsonNode outputPatch = applyLensToPatch(lens, patchForOriginalDoc);

        return JsonPatch.apply(outputPatch, base);
    }

    public static ArrayNode applyLensToPatch(List<LensOp> lens, ArrayNode patch) {
        Context ctx = new Context();
        Iterable<JsonNode> iterable = patch::elements;
        List<JsonNode> lensedPatch = StreamSupport.stream(iterable.spliterator(), false)
            .flatMap(op -> expandPatch(op).stream())
            .map(patchOp -> applyLensToPatchOp(ctx, lens, patchOp))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<JsonNode> lensedPatchWithDefaults = addDefaultValues(ctx, lensedPatch);

        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        result.addAll(lensedPatchWithDefaults);
        return result;
    }

    public static JsonNode applyLensToPatchOp(
        Context ctx, List<LensOp> lens, JsonNode patchOp) {
        return lens.stream()
            .reduce(patchOp,
                (prevPatch, lensOp) -> applyLensOp(ctx, lensOp, prevPatch),
                (prevPatch, currPatch) -> {
                    throw new UnsupportedOperationException(); // unused combiner
                }
            );
    }

    private static JsonNode applyLensOp(Context ctx, LensOp lensOp, JsonNode patchOp) {
        if (patchOp == null) {
            return null;
        }
        return lensOp.apply(ctx, patchOp);
    }

    protected static List<JsonNode> expandPatch(JsonNode patch) {
        String op = patch.get("op").textValue();
        if (!op.equals("add") && !op.equals("replace")) {
            return Collections.singletonList(patch);
        }

        JsonNode value = patch.get("value");
        if (value instanceof ObjectNode || value instanceof ArrayNode) {
            List<JsonNode> result = new ArrayList<>();
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.set("op", patch.get("op"));
            node.set("path", patch.get("path"));
            node.set("value", value instanceof ArrayNode
                ? JsonNodeFactory.instance.arrayNode()
                : JsonNodeFactory.instance.objectNode());
            result.add(node);

            Iterable<Map.Entry<String, JsonNode>> iterable = value::fields;
            List<JsonNode> expand = flatten(StreamSupport.stream(iterable.spliterator(), false)
                    .map(e -> {
                        ObjectNode n = JsonNodeFactory.instance.objectNode();
                        n.set("op", patch.get("op"));
                        n.set("path", JsonNodeFactory.instance.textNode(
                            patch.get("path").textValue() + "/" + e.getKey()));
                        n.set("value", e.getValue());
                        return expandPatch(n);
                    })
                    .collect(Collectors.toList()));
            result.addAll(expand);
            return result;
        }

        return Collections.singletonList(patch);
    }

    protected static List<JsonNode> addDefaultValues(Context ctx, List<JsonNode> patch) {
        return flatten(patch.stream()
            .map(patchOp -> {
                String op = patchOp.get("op").textValue();
                String path = patchOp.get("path").textValue();
                JsonNode value = patchOp.get("value");
                boolean isMakeMap = (op.equals("add") || op.equals("replace"))
                    && value instanceof ObjectNode
                    && !value.elements().hasNext();

                if (!isMakeMap) {
                    return patchOp;
                }

                Context subctx = getSubcontextForPath(ctx, path);

                return flatten(Stream.concat(
                        Stream.of(patchOp),
                        subctx.getSubcontexts().entrySet().stream()
                            .map(e -> {
                                String subpath = path + "/" + e.getKey();
                                Object defaultVal = e.getValue().getDefaultValue();
                                if (defaultVal != null) {
                                    ObjectNode copy = patchOp.deepCopy();
                                    copy.put("path", subpath);
                                    copy.put("value", valueToJsonNode(defaultVal));
                                    return addDefaultValues(ctx, Collections.singletonList(copy));
                                }
                                return Collections.emptyList();
                            })
                    )
                    .collect(Collectors.toList()));
            })
            .collect(Collectors.toList()));
    }

    protected static JsonNode valueToJsonNode(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof Byte) {
            return JsonNodeFactory.instance.numberNode(((Byte) value).intValue());
        } else if (value instanceof Short) {
            return JsonNodeFactory.instance.numberNode(((Short) value).intValue());
        } else if (value instanceof Integer) {
            return JsonNodeFactory.instance.numberNode(((Integer) value));
        } else if (value instanceof Long) {
            return JsonNodeFactory.instance.numberNode(((Long) value));
        } else if (value instanceof Float) {
            return JsonNodeFactory.instance.numberNode(((Float) value));
        } else if (value instanceof Double) {
            return JsonNodeFactory.instance.numberNode(((Double) value));
        } else if (value instanceof Boolean) {
            return JsonNodeFactory.instance.booleanNode(((Boolean) value));
        } else if (value instanceof String) {
            return JsonNodeFactory.instance.textNode(((String) value));
        } else if (value.getClass().isArray()) {
            // assume array is empty
            return JsonNodeFactory.instance.arrayNode();
        } else {
            // assume object is empty
            return JsonNodeFactory.instance.objectNode();
        }
    }

    protected static Context getSubcontextForPath(Context ctx, String path) {
        return Arrays.stream(path.split("/"))
            .skip(1)
            .reduce(ctx,
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

    @SuppressWarnings("unchecked")
    public static <T> List<T> flatten(Collection<?> collection) {
        List<Object> result = collection.stream()
                .flatMap(child -> {
                    if (child == null) {
                        return Stream.empty();
                    } else if (child instanceof Collection) {
                        return flatten((Collection<?>) child).stream();
                    } else {
                        return Stream.of(child);
                    }
                })
                .collect(Collectors.toList());
        return (List<T>) result;
    }

    public static JsonNode emptyDoc() {
        try {
            return Jackson.newObjectMapper().readTree("{}");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String [] args) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode source = om.readTree("{\"a\":  1, \"b\":  2}");
        JsonNode target = om.readTree("{\"c\":  1, \"d\":  2}");
        JsonNode patch = JsonDiff.asJson(source, target);
        System.out.println(patch);
    }
}
