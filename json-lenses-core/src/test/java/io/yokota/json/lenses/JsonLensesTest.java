package io.yokota.json.lenses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.ops.AddProperty;
import io.yokota.json.lenses.ops.ConvertValue;
import io.yokota.json.lenses.ops.LensIn;
import io.yokota.json.lenses.ops.LensMap;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.ops.RenameProperty;
import io.yokota.json.lenses.ops.ValueMapping;
import io.yokota.json.lenses.utils.Jackson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonLensesTest {

    private static final List<LensOp> lensSource = new ArrayList<>();

    @BeforeAll
    public static void init() {
        lensSource.add(new RenameProperty("title", "name"));
        lensSource.add(new AddProperty("description", ""));
        Map<Object, Object> forward = new HashMap<>();
        forward.put(false, "todo");
        forward.put(true, "done");
        Map<Object, Object> reverse = new HashMap<>();
        reverse.put("todo", false);
        reverse.put("inProgress", false);
        reverse.put("done", true);
        lensSource.add(new ConvertValue("complete", new ValueMapping(forward, reverse)));
    }

    @Test
    public void testFieldRename() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", " +
            "\"value\": \"new title\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        ObjectNode result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/name");
        assertThat(result.get("value").textValue()).isEqualTo("new title");

        patchStr = "{ \"op\": \"replace\", \"path\": \"/name\", " +
            "\"value\": \"new name\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/title");
        assertThat(result.get("value").textValue()).isEqualTo("new name");

        String docStr = "{ \"title\": \"hello\" }";
        JsonNode doc = Jackson.newObjectMapper().readTree(docStr);
        result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("description").textValue()).isEqualTo("");
        assertThat(result.get("name").textValue()).isEqualTo("hello");
    }

    @Test
    public void testAddField() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/description\", " +
            "\"value\": \"going swimmingly\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(
            JsonLenses.reverse(lensSource), patches);
        assertThat(lensedPatch.elements().hasNext()).isFalse();
    }

    @Test
    public void testDefaultValues() throws Exception {
        List<LensOp> lensSource = new ArrayList<>();
        AddProperty add1 = new AddProperty("tags", new ArrayList<>());
        lensSource.add(add1);
        AddProperty add2 = new AddProperty("name", "");
        AddProperty add3 = new AddProperty("color", "#ffffff");
        List<LensOp> list1 = new ArrayList<>();
        list1.add(add2);
        list1.add(add3);
        LensMap map1 = new LensMap(list1);
        LensIn in1 = new LensIn("tags", Collections.singletonList(map1));
        lensSource.add(in1);
        AddProperty add4 = new AddProperty("metadata", new Object());
        lensSource.add(add4);
        List<LensOp> list2 = new ArrayList<>();
        AddProperty add5 = new AddProperty("title", "");
        list2.add(add5);
        AddProperty add6 = new AddProperty("flags", "");
        list2.add(add6);
        AddProperty add7 = new AddProperty("O_CREATE", true);
        LensIn in2 = new LensIn("flags", Collections.singletonList(add7));
        list2.add(in2);
        LensIn in3 = new LensIn("metadata", list2);
        lensSource.add(in3);
        AddProperty add8 = new AddProperty("assignee", null);
        lensSource.add(add8);

        String patchStr = "{ \"op\": \"add\", \"path\": \"/tags/123\", " +
            "\"value\": { \"name\": \"bug\" } }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        JsonNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        System.out.println("*** " + lensedPatch);
    }

    @Test
    public void testPatchExpander() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": { \"a\": { \"b\": 5 } } }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);

        List<JsonNode> expanded = JsonLenses.expandPatch(patch);
        System.out.println("*** " + expanded);
    }
}
