package com.openai.config.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.openai.completion.ChatCompletionRequest;

public class ChatCompletionRequestSerializer extends JsonSerializer<ChatCompletionRequest.ChatCompletionRequestFunctionCall> {
    @Override
    public void serialize(ChatCompletionRequest.ChatCompletionRequestFunctionCall value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.getName() == null) {
            gen.writeNull();
        } else if ("none".equals(value.getName()) || "auto".equals(value.getName())) {
            gen.writeString(value.getName());
        } else {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(value.getName());
            gen.writeEndObject();
        }
    }
}
