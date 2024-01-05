package com.openai.config.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class ChatFunctionCallArgumentsDeserializer extends JsonDeserializer<JsonNode> {
    
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String json = p.getValueAsString();
        if (json == null || p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        try {
            JsonNode node = null;
            try {
                node = MAPPER.readTree(json);
            } catch (JsonParseException ignored) {
            }
            if (node == null || node.getNodeType() == JsonNodeType.MISSING) {
                node = MAPPER.readTree(p);
            }
            return node;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
