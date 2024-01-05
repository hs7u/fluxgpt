package com.openai.config.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.openai.completion.ChatCompletionRequest;

public class ChatCompletionRequestDeserializer extends JsonDeserializer<ChatCompletionRequest.ChatCompletionRequestFunctionCall> {
    @Override
    public ChatCompletionRequest.ChatCompletionRequestFunctionCall deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Check if the current JSON token represents the start of a structure (e.g., an object or array)
        if (p.getCurrentToken().isStructStart()) {
            p.nextToken(); //key
            p.nextToken(); //value
        }
        return new ChatCompletionRequest.ChatCompletionRequestFunctionCall(p.getValueAsString());
    }
}
