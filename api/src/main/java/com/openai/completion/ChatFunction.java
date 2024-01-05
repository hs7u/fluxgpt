package com.openai.completion;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

@Data
public class ChatFunction {

    // name：function的名稱
    @NonNull
    private String name;
    // description：對function的描述，也可以當作對於function執行時的Prompt
    private String description;
    // parameters：JSON Schema定義
    @JsonProperty("parameters")
    private Class<?> parametersClass;

    @JsonIgnore
    private Function<Object, Object> executor;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Class<?> parameters;
        private Function<Object, Object> executor;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Builder executor(Class<T> requestClass, Function<T, Object> executor) {
            this.parameters = requestClass;
            this.executor = (Function<Object, Object>) executor;
            return this;
        }

        public ChatFunction build() {
            ChatFunction chatFunction = new ChatFunction(name);
            chatFunction.setDescription(description);
            chatFunction.setParametersClass(parameters);
            chatFunction.setExecutor(executor);
            return chatFunction;
        }
    }
}
