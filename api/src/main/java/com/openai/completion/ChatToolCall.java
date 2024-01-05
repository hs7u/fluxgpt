package com.openai.completion;

import com.openai.constants.ToolType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatToolCall {
    
    /**
     * tool_call_id 
     */
    String id;

    /**
     * tool type
     */
    ToolType type;

    ChatFunctionCall function;
}
