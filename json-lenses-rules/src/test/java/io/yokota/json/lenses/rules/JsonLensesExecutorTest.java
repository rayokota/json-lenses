package io.yokota.json.lenses.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.entities.Rule;
import io.confluent.kafka.schemaregistry.rules.RuleContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonLensesExecutorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testRuleExecutor() throws Exception {
        String message = "{\"name\": \"Alice\",\"ssn\": \"123-45-6789\"}";
        JsonNode jsonNode = MAPPER.readTree(message);
        String expr = "[ { \"type\": \"rename\", \"source\": \"ssn\", \"target\": \"socialSecurityNumber\" } ]";
        Rule rule = new Rule(null, null, null, null, null, expr, null, null, false);
        RuleContext ctx = new RuleContext(null, null, null, null, null, null, false, null, rule);
        JsonLensesExecutor executor = new JsonLensesExecutor();
        Object output = executor.transform(ctx, jsonNode);
        String expected = "{\"name\":\"Alice\",\"socialSecurityNumber\":\"123-45-6789\"}";
        assertEquals(expected, output.toString());
    }
}
