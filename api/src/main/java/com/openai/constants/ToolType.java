package com.openai.constants;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ToolType {
    
    RETRIEVAL("retrieval"),
    FUNCTION("function");

    @JsonValue
    private String type;
}
