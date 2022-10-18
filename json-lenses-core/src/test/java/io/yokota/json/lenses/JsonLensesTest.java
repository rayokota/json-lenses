package io.yokota.json.lenses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.ops.AddProperty;
import io.yokota.json.lenses.ops.ConvertValue;
import io.yokota.json.lenses.ops.HoistProperty;
import io.yokota.json.lenses.ops.LensIn;
import io.yokota.json.lenses.ops.LensMap;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.ops.PlungeProperty;
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
        // converts upward
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

        // does not rename another property that starts with same string
        patchStr = "{ \"op\": \"replace\", \"path\": \"/title_bla\", " +
            "\"value\": \"new title\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/title_bla");
        assertThat(result.get("value").textValue()).isEqualTo("new title");

        // converts downwards
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

        // works with whole doc conversion too
        String docStr = "{ \"title\": \"hello\" }";
        JsonNode doc = Jackson.newObjectMapper().readTree(docStr);
        result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("description").textValue()).isEqualTo("");
        assertThat(result.get("name").textValue()).isEqualTo("hello");
    }

    @Test
    public void testAddField() throws Exception {
        // becomes an empty patch when reversed
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
    public void testValueConversion() throws Exception {
        // converts from a boolean to a string enum
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", " +
            "\"value\": true }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        ObjectNode result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/complete");
        assertThat(result.get("value").textValue()).isEqualTo("done");

        // reverse converts from a string enum to a boolean
        patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", " +
            "\"value\": \"inProgress\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/complete");
        assertThat(result.get("value").booleanValue()).isFalse();

        // handles a value conversion and a rename in the same lens
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new RenameProperty("complete", "status"));
        Map<Object, Object> forward = new HashMap<>();
        forward.put(false, "todo");
        forward.put(true, "done");
        Map<Object, Object> reverse = new HashMap<>();
        reverse.put("todo", false);
        reverse.put("inProgress", false);
        reverse.put("done", true);
        lensSource.add(new ConvertValue("status", new ValueMapping(forward, reverse)));

        patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", " +
            "\"value\": true }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/status");
        assertThat(result.get("value").textValue()).isEqualTo("done");
    }

    @Test
    public void testSinglyNestedObject() throws Exception {
        // renaming metadata/basic/title to metadata/basic/name. a more sugary syntax:
        // in("metadata", rename("title", "name", "string"))
        List<LensOp> lensSource = new ArrayList<>();
        RenameProperty rename1 = new RenameProperty("title", "name");
        lensSource.add(new LensIn("metadata", Collections.singletonList(rename1)));

        // renames a field correctly
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata/name\", " +
            "\"value\": \"hello\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        ObjectNode result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/metadata/name");
        assertThat(result.get("value").textValue()).isEqualTo("hello");

        // works with whole doc conversion
        String docStr = "{ \"metadata\": { \"title\": \"hello\" } }";
        JsonNode doc = Jackson.newObjectMapper().readTree(docStr);
        result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("metadata").get("name").textValue()).isEqualTo("hello");

        // doesn't rename another field
        patchStr = "{ \"op\": \"replace\", \"path\": \"/otherparent/title\", " +
            "\"value\": \"hello\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/otherparent/title");
        assertThat(result.get("value").textValue()).isEqualTo("hello");

        // renames a field in the left direction
        patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata/name\", " +
            "\"value\": \"hello\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/metadata/title");
        assertThat(result.get("value").textValue()).isEqualTo("hello");

        // renames the field when a whole object is set in a patch
        patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata\", " +
            "\"value\": { \"title\": \"hello\" } }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/metadata");
        assertThat(result.get("value")).isInstanceOf(ObjectNode.class);
        result = (ObjectNode) lensedPatch.get(1);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/metadata/name");
        assertThat(result.get("value").textValue()).isEqualTo("hello");
    }

    @Test
    public void testArrays() throws Exception {
        // renaming tasks/n/title to tasks/n/name
        List<LensOp> lensSource = new ArrayList<>();
        RenameProperty rename1 = new RenameProperty("title", "name");
        LensMap map1 = new LensMap(Collections.singletonList(rename1));
        lensSource.add(new LensIn("tasks", Collections.singletonList(map1)));

        // renames a field in an array element
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/tasks/23/title\", " +
            "\"value\": \"hello\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        ObjectNode result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/tasks/23/name");
        assertThat(result.get("value").textValue()).isEqualTo("hello");

        // renames a field in the left direction
        patchStr = "{ \"op\": \"replace\", \"path\": \"/tasks/23/name\", " +
            "\"value\": \"hello\" }";
        patch = Jackson.newObjectMapper().readTree(patchStr);
        patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/tasks/23/title");
        assertThat(result.get("value").textValue()).isEqualTo("hello");
    }

    @Test
    public void testHoist() throws Exception {
        // pulls a field up to its parent
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new HoistProperty("metadata", "createdAt"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata/createdAt\", " +
            "\"value\": \"July 7th, 2020\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        ObjectNode result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/createdAt");
        assertThat(result.get("value").textValue()).isEqualTo("July 7th, 2020");
    }

    @Test
    public void testPlunge() throws Exception {
        // pushes a field into a child with applyLensToDoc
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new PlungeProperty("tags", "color"));

        String docStr = "{ \"tags\": {}, \"color\": \"orange\" }";
        // TODO fix
        //String docStr = "{ \"color\": \"orange\", \"tags\": { } }";
        JsonNode doc = Jackson.newObjectMapper().readTree(docStr);
        ObjectNode result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("tags").get("color").textValue()).isEqualTo("orange");

        // pushes a field into its child
        lensSource = new ArrayList<>();
        lensSource.add(new PlungeProperty("metadata", "title"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", " +
            "\"value\": \"Fun project\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        result = (ObjectNode) lensedPatch.get(0);
        assertThat(result.get("op").textValue()).isEqualTo("replace");
        assertThat(result.get("path").textValue()).isEqualTo("/metadata/title");
        assertThat(result.get("value").textValue()).isEqualTo("Fun project");
    }

    @Test
    public void testDefaultValues() throws Exception {
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new AddProperty("tags", new ArrayList<>()));
        List<LensOp> list1 = new ArrayList<>();
        list1.add(new AddProperty("name", ""));
        list1.add(new AddProperty("color", "#ffffff"));
        LensMap map1 = new LensMap(list1);
        lensSource.add(new LensIn("tags", Collections.singletonList(map1)));
        lensSource.add(new AddProperty("metadata", new Object()));
        List<LensOp> list2 = new ArrayList<>();
        list2.add(new AddProperty("title", ""));
        list2.add(new AddProperty("flags", ""));
        list2.add(new LensIn("flags", Collections.singletonList(new AddProperty("O_CREATE", true))));
        lensSource.add(new LensIn("metadata", list2));
        lensSource.add(new AddProperty("assignee", null));

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
