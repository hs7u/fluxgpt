package com.openai.completion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openai.constants.ToolType;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatTool {
    @JsonProperty("tool_call_id")
    private String toolCallId;
    
    private ToolType type;
    
    private ChatFunction function;
}
