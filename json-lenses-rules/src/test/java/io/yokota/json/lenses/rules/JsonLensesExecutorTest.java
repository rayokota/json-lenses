package io.yokota.json.lenses.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.entities.Rule;
import io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode;
import io.confluent.kafka.schemaregistry.rules.RuleContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonLensesExecutorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testRuleExecutor() throws Exception {
        String original = "{\"name\":\"Alice\",\"ssn\":\"123-45-6789\"}";
        JsonNode jsonNode = MAPPER.readTree(original);
        String expr = "[ { \"type\": \"rename\", \"source\": \"ssn\", \"target\": \"socialSecurityNumber\" } ]";
        Rule rule = new Rule(null, null, null, null, null, null, null, expr, null, null, false);
        RuleContext ctx = new RuleContext(null, null, null, null, null, null, null, false,
            RuleMode.UPGRADE, rule, 0, Collections.singletonList(rule));
        JsonLensesExecutor executor = new JsonLensesExecutor();
        JsonNode upgraded = (JsonNode) executor.transform(ctx, jsonNode);
        String expected = "{\"name\":\"Alice\",\"socialSecurityNumber\":\"123-45-6789\"}";
        assertEquals(expected, upgraded.toString());

        ctx = new RuleContext(null, null, null, null, null, null, null, false,
            RuleMode.DOWNGRADE, rule, 0, Collections.singletonList(rule));
        JsonNode downgraded = (JsonNode) executor.transform(ctx, upgraded);
        assertEquals(original, downgraded.toString());
    }
}
