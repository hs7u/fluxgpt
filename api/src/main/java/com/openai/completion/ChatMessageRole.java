package com.openai.completion;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * see {@link ChatMessage} documentation.
 */
public enum ChatMessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    FUNCTION("function"),
    TOOL("tool");

    private final String value;

    ChatMessageRole(final String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
