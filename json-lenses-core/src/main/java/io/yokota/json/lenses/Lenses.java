package io.yokota.json.lenses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.utils.Jackson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Lenses {

    private static final JsonNode EMPTY_DOC = emptyDoc();

    public static JsonNode applyLensToDoc(
        List<LensOp> lens, JsonNode inputDoc, JsonNode targetDoc) {
        ArrayNode patchForOriginalDoc = (ArrayNode) JsonDiff.asJson(EMPTY_DOC, inputDoc);

        JsonNode base = targetDoc != null ? targetDoc : emptyDoc();
        JsonNode outputPatch = applyLensToPatch(lens, patchForOriginalDoc);

        return JsonPatch.apply(outputPatch, base);
    }

    public static ArrayNode applyLensToPatch(List<LensOp> lens, ArrayNode patch) {
        Iterable<JsonNode> iterable = patch::elements;
        List<JsonNode> lensedPatch = StreamSupport.stream(iterable.spliterator(), false)
            .flatMap(op -> expandPatch(op).stream())
            .map(patchOp -> applyLensToPatchOp(lens, patchOp))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        result.addAll(lensedPatch);
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

    public static List<JsonNode> expandPatch(JsonNode patch) {
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


    private static List<JsonNode> flatten(List<?> list) {
        if (list.get(0) instanceof JsonNode) {
            return (List<JsonNode>) list;
        }

        List<List<?>> listOfLists = (List<List<?>>) list;
        return flatten(listOfLists.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
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
