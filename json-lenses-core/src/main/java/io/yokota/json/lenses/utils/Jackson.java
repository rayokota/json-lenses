package io.yokota.json.lenses.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

/**
 * A utility class for Jackson.
 */
public class Jackson {
    private Jackson() {
        /* singleton */
    }

    /**
     * Creates a new {@link ObjectMapper}.
     */
    public static ObjectMapper newObjectMapper() {
        final ObjectMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

        return configure(mapper);
    }

    /**
     * Creates a new {@link ObjectMapper} with a custom
     * {@link JsonFactory}.
     *
     * @param jsonFactory instance of {@link JsonFactory} to use
     *                    for the created {@link ObjectMapper} instance.
     */
    public static ObjectMapper newObjectMapper(JsonFactory jsonFactory) {
        final ObjectMapper mapper = JsonMapper.builder(jsonFactory)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

        return configure(mapper);
    }

    private static ObjectMapper configure(ObjectMapper mapper) {
        return mapper;
    }

    public static JsonNode merge(JsonNode target, JsonNode source) {

        Iterator<String> fieldNames = source.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = target.get(updatedFieldName);
            JsonNode updatedValue = source.get(updatedFieldName);

            // If the node is an ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() &&
                updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (target instanceof ObjectNode) {
                    ((ObjectNode) target).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return target;
    }
}
