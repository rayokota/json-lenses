package io.yokota.json.lenses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.yokota.json.lenses.ops.AddProperty;
import io.yokota.json.lenses.ops.ConvertValue;
import io.yokota.json.lenses.ops.HeadProperty;
import io.yokota.json.lenses.ops.HoistProperty;
import io.yokota.json.lenses.ops.LensIn;
import io.yokota.json.lenses.ops.LensMap;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.ops.PlungeProperty;
import io.yokota.json.lenses.ops.RenameProperty;
import io.yokota.json.lenses.ops.ValueMapping;
import io.yokota.json.lenses.ops.WrapProperty;
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

    private static final ObjectMapper mapper = Jackson.newObjectMapper();

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
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", \"value\": \"new title\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/name", "new title");

        // does not rename another property that starts with same string
        patchStr = "{ \"op\": \"replace\", \"path\": \"/title_bla\", \"value\": \"new title\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/title_bla", "new title");

        // converts downwards
        patchStr = "{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"new name\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/title", "new name");

        // works with whole doc conversion too
        String docStr = "{ \"title\": \"hello\" }";
        JsonNode doc = mapper.readTree(docStr);
        ObjectNode result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("description").textValue()).isEqualTo("");
        assertThat(result.get("name").textValue()).isEqualTo("hello");
    }

    @Test
    public void testAddField() throws Exception {
        // becomes an empty patch when reversed
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/description\", " +
            "\"value\": \"going swimmingly\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(
            JsonLenses.reverse(lensSource), patches);
        assertThat(lensedPatch.elements().hasNext()).isFalse();
    }

    @Test
    public void testValueConversion() throws Exception {
        // converts from a boolean to a string enum
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", \"value\": true }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/complete", "done");

        // reverse converts from a string enum to a boolean
        patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", \"value\": \"inProgress\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/complete", false);

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

        patchStr = "{ \"op\": \"replace\", \"path\": \"/complete\", \"value\": true }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/status", "done");
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
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/metadata/name", "hello");

        // works with whole doc conversion
        String docStr = "{ \"metadata\": { \"title\": \"hello\" } }";
        JsonNode doc = mapper.readTree(docStr);
        ObjectNode result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("metadata").get("name").textValue()).isEqualTo("hello");

        // doesn't rename another field
        patchStr = "{ \"op\": \"replace\", \"path\": \"/otherparent/title\", " +
            "\"value\": \"hello\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/otherparent/title", "hello");

        // renames a field in the left direction
        patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata/name\", \"value\": \"hello\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/metadata/title", "hello");

        // renames the field when a whole object is set in a patch
        patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata\", " +
            "\"value\": { \"title\": \"hello\" } }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/metadata", new Object());
        checkPatch((ObjectNode) lensedPatch.get(1), "replace", "/metadata/name", "hello");
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
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/tasks/23/name", "hello");

        // renames a field in the left direction
        patchStr = "{ \"op\": \"replace\", \"path\": \"/tasks/23/name\", \"value\": \"hello\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/tasks/23/title", "hello");
    }

    @Test
    public void testHoist() throws Exception {
        // pulls a field up to its parent
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new HoistProperty("metadata", "createdAt"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/metadata/createdAt\", " +
            "\"value\": \"July 7th, 2020\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/createdAt", "July 7th, 2020");
    }

    @Test
    public void testPlunge() throws Exception {
        // pushes a field into a child with applyLensToDoc
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new PlungeProperty("tags", "color"));

        String docStr = "{ \"tags\": {}, \"color\": \"orange\" }";
        // TODO fix
        //String docStr = "{ \"color\": \"orange\", \"tags\": { } }";
        JsonNode doc = mapper.readTree(docStr);
        ObjectNode result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        assertThat(result.get("tags").get("color").textValue()).isEqualTo("orange");

        // pushes a field into its child
        lensSource = new ArrayList<>();
        lensSource.add(new PlungeProperty("metadata", "title"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", " +
            "\"value\": \"Fun project\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/metadata/title", "Fun project");
    }

    @Test
    public void testWrap() throws Exception {
        //converts head replace value into 0th element writes into its child
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new WrapProperty("assignee"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee\", " +
            "\"value\": \"July 7th, 2020\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee/0", "July 7th, 2020");

        // converts head add value into 0th element writes into its child
        patchStr = "{ \"op\": \"add\", \"path\": \"/assignee\", " +
            "\"value\": \"July 7th, 2020\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "/assignee/0", "July 7th, 2020");

        // converts head null write into a remove the first element op
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee\", \"value\": null }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "remove", "/assignee/0", null);

        // handles a wrap followed by a rename
        lensSource = new ArrayList<>();
        lensSource.add(new WrapProperty("assignee"));
        lensSource.add(new RenameProperty("assignee", "assignees"));

        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee\", \"value\": \"pvh\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignees/0", "pvh");

        // converts nested values into 0th element writes into its child
        lensSource = new ArrayList<>();
        lensSource.add(new WrapProperty("assignee"));

        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/name\", \"value\": \"Orion\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee/0/name", "Orion");

        // converts array first element write into a write on the scalar
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/0\", " +
            "\"value\": \"July 7th, 2020\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee", "July 7th, 2020");
    }

    @Test
    public void testHead() throws Exception {
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new HeadProperty("assignee"));

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/0\", " +
            "\"value\": \"Peter\" }";
        ArrayNode patches = createPatch(patchStr);

        ArrayNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee", "Peter");

        // converts a write on other elements to a no-op
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/1\", " +
            "\"value\": \"Peter\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        assertThat(lensedPatch.elements().hasNext()).isFalse();

        // converts array first element delete into a null write on the scalar
        patchStr = "{ \"op\": \"remove\", \"path\": \"/assignee/0\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee", NullNode.instance);

        // preserves the rest of the path after the array index
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/0/age\", \"value\": 23 }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee/age", 23);

        // preserves the rest of the path after the array index with nulls
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/0/age\", \"value\": null }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee/age", NullNode.instance);

        // preserves the rest of the path after the array index with removes
        patchStr = "{ \"op\": \"remove\", \"path\": \"/assignee/0/age\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "remove", "/assignee/age", null);

        // correctly handles a sequence of array writes
        patchStr = "{ \"op\": \"add\", \"path\": \"/assignee/0\", \"value\": \"geoffrey\" }";
        patches = createPatch(patchStr);
        patchStr = "{ \"op\": \"add\", \"path\": \"/assignee/1\", \"value\": \"orion\" }";
        addPatch(patches, patchStr);
        patchStr = "{ \"op\": \"remove\", \"path\": \"/assignee/1\" }";
        addPatch(patches, patchStr);
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee/0\", \"value\": \"orion\" }";
        addPatch(patches, patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "/assignee", "geoffrey");
        checkPatch((ObjectNode) lensedPatch.get(1), "replace", "/assignee", "orion");

        // converts head set value into 0th element writes into its child
        patchStr = "{ \"op\": \"replace\", \"path\": \"/assignee\", \"value\": \"July 7th, 2020\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(JsonLenses.reverse(lensSource), patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "replace", "/assignee/0", "July 7th, 2020");
    }

    @Test
    public void testPatchExpander() throws Exception {
        // expands a patch that sets an object
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": { \"a\": { \"b\": 5 } } }";
        JsonNode patch = mapper.readTree(patchStr);

        List<JsonNode> expanded = JsonLenses.expandPatch(patch);
        checkPatch((ObjectNode) expanded.get(0), "replace", "/obj", new Object());
        checkPatch((ObjectNode) expanded.get(1), "replace", "/obj/a", new Object());
        checkPatch((ObjectNode) expanded.get(2), "replace", "/obj/a/b", 5);

        // works with multiple keys
        patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": { \"a\": { \"b\": 5, \"c\": { \"d\": 6 } } } }";
        patch = mapper.readTree(patchStr);

        expanded = JsonLenses.expandPatch(patch);
        checkPatch((ObjectNode) expanded.get(0), "replace", "/obj", new Object());
        checkPatch((ObjectNode) expanded.get(1), "replace", "/obj/a", new Object());
        checkPatch((ObjectNode) expanded.get(2), "replace", "/obj/a/b", 5);
        checkPatch((ObjectNode) expanded.get(3), "replace", "/obj/a/c", new Object());
        checkPatch((ObjectNode) expanded.get(4), "replace", "/obj/a/c/d", 6);

        // expands a patch that sets an array
        patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": [ \"hello\", \"world\" ] }";
        patch = mapper.readTree(patchStr);

        expanded = JsonLenses.expandPatch(patch);
        checkPatch((ObjectNode) expanded.get(0), "replace", "/obj", new Object[0]);
        checkPatch((ObjectNode) expanded.get(1), "replace", "/obj/0", "hello");
        checkPatch((ObjectNode) expanded.get(2), "replace", "/obj/1", "world");

        // works recursively with objects and arrays
        patchStr = "{ \"op\": \"replace\", \"path\": \"\", " +
            "\"value\": { \"tasks\": [ { \"name\": \"hello\" }, { \"name\": \"world\" } ] } }";
        patch = mapper.readTree(patchStr);

        expanded = JsonLenses.expandPatch(patch);
        checkPatch((ObjectNode) expanded.get(0), "replace", "", new Object());
        checkPatch((ObjectNode) expanded.get(1), "replace", "/tasks", new Object[0]);
        checkPatch((ObjectNode) expanded.get(2), "replace", "/tasks/0", new Object());
        checkPatch((ObjectNode) expanded.get(3), "replace", "/tasks/0/name", "hello");
        checkPatch((ObjectNode) expanded.get(4), "replace", "/tasks/1", new Object());
        checkPatch((ObjectNode) expanded.get(5), "replace", "/tasks/1/name", "world");
    }

    @Test
    public void testDefaultValues() throws Exception {
        List<LensOp> lensSource = new ArrayList<>();
        lensSource.add(new AddProperty("tags", new Object[0]));
        List<LensOp> list1 = new ArrayList<>();
        list1.add(new AddProperty("name", ""));
        list1.add(new AddProperty("color", "#ffffff"));
        LensMap map1 = new LensMap(list1);
        lensSource.add(new LensIn("tags", Collections.singletonList(map1)));
        lensSource.add(new AddProperty("metadata", new Object()));
        List<LensOp> list2 = new ArrayList<>();
        list2.add(new AddProperty("title", ""));
        list2.add(new AddProperty("flags", new Object()));
        list2.add(new LensIn("flags", Collections.singletonList(new AddProperty("O_CREATE", true))));
        lensSource.add(new LensIn("metadata", list2));
        lensSource.add(new AddProperty("assignee", null));

        // fills in defaults on a patch that adds a new array item
        String patchStr = "{ \"op\": \"add\", \"path\": \"/tags/123\", " +
            "\"value\": { \"name\": \"bug\" } }";
        ArrayNode patches = createPatch(patchStr);

        JsonNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "/tags/123", new Object());
        checkPatch((ObjectNode) lensedPatch.get(1), "add", "/tags/123/color", "#ffffff");
        checkPatch((ObjectNode) lensedPatch.get(2), "add", "/tags/123/name", "");
        checkPatch((ObjectNode) lensedPatch.get(3), "add", "/tags/123/name", "bug");

        // doesn't expand a patch on an object key that already exists
        patchStr = "{ \"op\": \"add\", \"path\": \"/tags/123/name\", " +
            "\"value\": \"bug\" }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "/tags/123/name", "bug");

        // recursively fills in defaults from the root
        patchStr = "{ \"op\": \"add\", \"path\": \"\", \"value\": {} }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "", new Object());
        checkPatch((ObjectNode) lensedPatch.get(1), "add", "/metadata", new Object());
        checkPatch((ObjectNode) lensedPatch.get(2), "add", "/metadata/flags", new Object());
        checkPatch((ObjectNode) lensedPatch.get(3), "add", "/metadata/flags/O_CREATE", true);
        checkPatch((ObjectNode) lensedPatch.get(4), "add", "/metadata/title", "");
        checkPatch((ObjectNode) lensedPatch.get(5), "add", "/tags", new Object[0]);

        // works correctly when properties are spread across multiple lenses
        List<LensOp> v1Tov2Lens = new ArrayList<>(lensSource);  // append to lensSource
        v1Tov2Lens.add(new RenameProperty("tags", "labels"));
        AddProperty add1 = new AddProperty("important", false);
        LensMap map2 = new LensMap(Collections.singletonList(add1));
        v1Tov2Lens.add(new LensIn("labels", Collections.singletonList(map2)));

        patchStr = "{ \"op\": \"add\", \"path\": \"/tags/123\", \"value\": { \"name\": \"bug\" } }";
        patches = createPatch(patchStr);

        lensedPatch = JsonLenses.applyLensToPatch(v1Tov2Lens, patches);
        checkPatch((ObjectNode) lensedPatch.get(0), "add", "/labels/123", new Object());
        checkPatch((ObjectNode) lensedPatch.get(1), "add", "/labels/123/important", false);
        checkPatch((ObjectNode) lensedPatch.get(2), "add", "/labels/123/color", "#ffffff");
        checkPatch((ObjectNode) lensedPatch.get(3), "add", "/labels/123/name", "");
        checkPatch((ObjectNode) lensedPatch.get(4), "add", "/labels/123/name", "bug");
    }

    @Test
    public void testInferringFromDocuments() throws Exception {
        String docStr = "{ \"name\": \"hello\", \"details\": { \"age\": 23, \"height\": 64 } }";
        JsonNode doc = mapper.readTree(docStr);

        List<LensOp> lensSource = new ArrayList<>();
        RenameProperty rename1 = new RenameProperty("height", "heightInches");
        lensSource.add(new LensIn("details", Collections.singletonList(rename1)));

        ObjectNode result = (ObjectNode) JsonLenses.applyLensToDoc(lensSource, doc, null);
        docStr = "{ \"name\": \"hello\", \"details\": { \"age\": 23, \"heightInches\": 64 } }";
        doc = mapper.readTree(docStr);
        assertThat(result).isEqualTo(doc);
    }

    private ArrayNode createPatch(String patchStr) throws JsonProcessingException {
        JsonNode patch = mapper.readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);
        return patches;
    }

    private void addPatch(ArrayNode patches, String patchStr) throws JsonProcessingException {
        JsonNode patch = mapper.readTree(patchStr);
        patches.add(patch);
    }

    private void checkPatch(ObjectNode patch, String op, String path, Object value) {
        assertThat(patch.get("op").textValue()).isEqualTo(op);
        assertThat(patch.get("path").textValue()).isEqualTo(path);
        if (value == null) {
            return;
        } else if (value instanceof NullNode) {
            assertThat(patch.get("value").isNull()).isTrue();
        } else if (value instanceof Number) {
            assertThat(patch.get("value").numberValue()).isEqualTo(value);
        } else if (value instanceof Boolean) {
            assertThat(patch.get("value").booleanValue()).isEqualTo(value);
        } else if (value instanceof String) {
            assertThat(patch.get("value").textValue()).isEqualTo(value);
        } else if (value.getClass().isArray()) {
            assertThat(patch.get("value").isArray()).isTrue();
        } else {
            assertThat(patch.get("value").isObject()).isTrue();
        }
    }
}
