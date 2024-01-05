package com.openai.completion;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class ChatMessage {
	
	@NonNull
	ChatMessageRole role;
	@JsonInclude 
	String content;
	String name;

	@Deprecated
	@JsonProperty("function_call")
	ChatFunctionCall functionCall;

	String toolCallId;
	@JsonProperty("tool_calls")
	List<ChatToolCall> toolCalls;

	public ChatMessage(ChatMessageRole role, String content) {
		this.role = role;
		this.content = content;
	}

	public ChatMessage(ChatMessageRole role, String content, String name) {
		this.role = role;
		this.content = content;
		this.name = name;
	}

	public ChatMessage(ChatMessageRole role, String content, String name, String toolCallId) {
		this.role = role;
		this.content = content;
		this.name = name;
		this.toolCallId = toolCallId;
	}

}
