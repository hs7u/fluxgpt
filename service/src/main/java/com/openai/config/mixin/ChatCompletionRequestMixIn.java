package com.openai.config.mixin;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.openai.completion.ChatCompletionRequest;
import com.openai.config.deserializer.ChatCompletionRequestDeserializer;
import com.openai.config.serializer.ChatCompletionRequestSerializer;

public abstract class ChatCompletionRequestMixIn {

    @JsonSerialize(using = ChatCompletionRequestSerializer.class)
    @JsonDeserialize(using = ChatCompletionRequestDeserializer.class)
    abstract ChatCompletionRequest.ChatCompletionRequestFunctionCall getFunctionCall();

}

