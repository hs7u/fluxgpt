package com.openai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.completion.ChatMessage;
import com.openai.completion.ChatMessageRole;
import com.openai.completion.ChatTool;
import com.openai.completion.ChatToolCall;
import com.openai.constants.ToolType;

public class FunctionExecutor {
    private ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ChatFunction> FUNCTIONS = new HashMap<>();
    private final List<ChatTool> TOOLS = new ArrayList<>();

    public FunctionExecutor(List<ChatFunction> functions) {
        setFunctions(functions);
        setTools(functions);
    }

    public FunctionExecutor(List<ChatFunction> functions, ObjectMapper objectMapper) {
        setFunctions(functions);
        setObjectMapper(objectMapper);
    }

    public Optional<ChatMessage> executeAndConvertToMessageSafely(ChatFunctionCall call) {
        try {
            return Optional.ofNullable(executeAndConvertToMessage(call));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
    public Optional<ChatMessage> executeAndConvertToMessageSafely(ChatToolCall call) {
        try {
            return Optional.ofNullable(executeAndConvertToMessage(call));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public ChatMessage executeAndConvertToMessageHandlingExceptions(ChatFunctionCall call) {
        try {
            return executeAndConvertToMessage(call);
        } catch (Exception exception) {
            exception.printStackTrace();
            return convertExceptionToMessage(exception);
        }
    }

    public ChatMessage convertExceptionToMessage(Exception exception) {
        String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
        return new ChatMessage(ChatMessageRole.FUNCTION, "{\"error\": \"" + error + "\"}", "error");
    }

    public ChatMessage executeAndConvertToMessage(ChatFunctionCall call) {
        return new ChatMessage(ChatMessageRole.FUNCTION, executeAndConvertToJson(call).toPrettyString(), call.getName());
    }

    public ChatMessage executeAndConvertToMessage(ChatToolCall tool) {
        ChatFunctionCall call = tool.getFunction();
        return new ChatMessage(ChatMessageRole.TOOL, executeAndConvertToJson(call).toPrettyString(), call.getName(), tool.getId());
    }

    public JsonNode executeAndConvertToJson(ChatFunctionCall call) {
        try {
            Object execution = execute(call);
            if (execution instanceof TextNode) {
                JsonNode objectNode = mapper.readTree(((TextNode) execution).asText());
                if (objectNode.isMissingNode())
                    return (JsonNode) execution;
                return objectNode;
            }
            if (execution instanceof ObjectNode) {
                return (JsonNode) execution;
            }
            if (execution instanceof String) {
                JsonNode objectNode = mapper.readTree((String) execution);
                if (objectNode.isMissingNode())
                    throw new RuntimeException("Parsing exception");
                return objectNode;
            }
            return mapper.readValue(mapper.writeValueAsString(execution), JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(ChatFunctionCall call) {
        ChatFunction function = FUNCTIONS.get(call.getName());
        Object obj;
        try {
            JsonNode arguments = call.getArguments();
            obj = mapper.readValue(arguments instanceof TextNode ? arguments.asText() : arguments.toPrettyString(), function.getParametersClass());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return (T) function.getExecutor().apply(obj);
    }

    public List<ChatFunction> getFunctions() {
        return new ArrayList<>(FUNCTIONS.values());
    }

    public void setTools(List<ChatFunction> functions) {
        this.TOOLS.clear();
        functions.forEach(f -> this.TOOLS.add(ChatTool.builder().type(ToolType.FUNCTION).function(f).build()));
    }

    public List<ChatTool> getTools() {
        return this.TOOLS;
    }

    public void setFunctions(List<ChatFunction> functions) {
        this.FUNCTIONS.clear();
        functions.forEach(f -> this.FUNCTIONS.put(f.getName(), f));
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

}
