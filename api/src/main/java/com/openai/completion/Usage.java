package com.openai.completion;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * The OpenAI resources used by a request
 */
@Data
public class Usage {
     
    @JsonProperty("prompt_tokens")
    long promptTokens;

    @JsonProperty("completion_tokens")
    long completionTokens;

    @JsonProperty("total_tokens")
    long totalTokens;
}
