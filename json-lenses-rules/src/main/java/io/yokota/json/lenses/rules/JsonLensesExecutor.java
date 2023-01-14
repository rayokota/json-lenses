package io.yokota.json.lenses.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode;
import io.confluent.kafka.schemaregistry.rules.RuleContext;
import io.confluent.kafka.schemaregistry.rules.RuleException;
import io.confluent.kafka.schemaregistry.rules.RuleExecutor;
import io.yokota.json.lenses.JsonLenses;
import io.yokota.json.lenses.ops.LensOp;
import org.apache.avro.data.Json;

import java.util.List;

public class JsonLensesExecutor implements RuleExecutor {

    public static final String TYPE = "JSONLENSES";

    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Object transform(RuleContext ctx, Object message)
        throws RuleException {
        JsonNode jsonObj = (JsonNode) message;

        try {
            String expr = ctx.rule().getExpr();
            List<LensOp> lens = MAPPER.readValue(expr, new TypeReference<>() {});
            if (ctx.ruleMode() == RuleMode.DOWNGRADE) {
                lens = JsonLenses.reverse(lens);
            }
            return JsonLenses.applyLensToDoc(lens, jsonObj, null);
        } catch (JsonProcessingException e) {
            throw new RuleException("Could not parse rule", e);
        }
    }
}
