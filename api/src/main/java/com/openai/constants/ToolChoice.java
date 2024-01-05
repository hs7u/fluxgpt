package com.openai.constants;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ToolChoice {
    
    NONE("none"),
    AUTO("auto");

    @JsonValue
    private String name;
}
