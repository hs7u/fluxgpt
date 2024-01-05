package com.openai.config.mixin;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.openai.config.serializer.ChatFunctionParametersSerializer;

public abstract class ChatFunctionMixIn {

    @JsonSerialize(using = ChatFunctionParametersSerializer.class)
    abstract Class<?> getParametersClass();

}
