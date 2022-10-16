package io.yokota.json.lenses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.yokota.json.lenses.ops.LensOp;
import io.yokota.json.lenses.ops.RenameProperty;
import io.yokota.json.lenses.utils.Jackson;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class LensesTest {

    @Test
    public void testPatchFieldRename() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/title\", " +
            "\"value\": \"new title\" }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);
        ArrayNode patches = JsonNodeFactory.instance.arrayNode();
        patches.add(patch);

        LensOp rename = new RenameProperty("title", "name");
        List<LensOp> lensSource = Collections.singletonList(rename);
        JsonNode lensedPatch = Lenses.applyLensToPatch(lensSource, patches);

        System.out.println("*** " + lensedPatch);
    }

    @Test
    public void testExpandPatch() throws Exception {
        String patchStr = "{ \"op\": \"replace\", \"path\": \"/obj\", " +
            "\"value\": { \"a\": { \"b\": 5 } } }";
        JsonNode patch = Jackson.newObjectMapper().readTree(patchStr);

        List<JsonNode> expanded = Lenses.expandPatch(patch);
        System.out.println("*** " + expanded);
    }
}
