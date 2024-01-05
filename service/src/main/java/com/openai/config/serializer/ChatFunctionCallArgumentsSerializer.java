package com.openai.config.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.TextNode;

public class ChatFunctionCallArgumentsSerializer extends JsonSerializer<JsonNode> {
    
    @Override
    public void serialize(JsonNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value instanceof TextNode ? value.asText() : value.toPrettyString());
        }
    }
}
