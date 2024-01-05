package com.openai.config.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.openai.config.deserializer.ChatFunctionCallArgumentsDeserializer;
import com.openai.config.serializer.ChatFunctionCallArgumentsSerializer;

public abstract class ChatFunctionCallMixIn {

    @JsonSerialize(using = ChatFunctionCallArgumentsSerializer.class)
    @JsonDeserialize(using = ChatFunctionCallArgumentsDeserializer.class)
    abstract JsonNode getArguments();

}
