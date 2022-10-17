package io.yokota.json.lenses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.yokota.json.lenses.ops.AddProperty;
import io.yokota.json.lenses.ops.LensIn;
import io.yokota.json.lenses.ops.LensMap;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.ops.RenameProperty;
import io.yokota.json.lenses.utils.Jackson;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonLensesTest {

    @Test
    public void testPatchFieldRename() throws Exception {
        LensOp rename = new RenameProperty("title", "name");
        List<LensOp> lensSource = Collections.singletonList(rename);

        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", " +
            "\"value\": \"new title\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        JsonNode lensedPatch = JsonLenses.applyLensToPatch(lensSource, patches);
        System.out.println("*** " + lensedPatch);
    }

    @Test
    public void testDefaults() throws Exception {
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
    public void testExpandPatch() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": { \"a\": { \"b\": 5 } } }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);

        List<JsonNode> expanded = JsonLenses.expandPatch(patch);
        System.out.println("*** " + expanded);
    }
}
