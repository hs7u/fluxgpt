package com.openai;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import com.openai.completion.ChatCompletionRequest;
import com.openai.completion.ChatCompletionResult;

import reactor.core.publisher.Mono;
// import feign.RequestLine;

public interface OpenAiApi {

    @PostExchange("/v1/chat/completions")
    Mono<ChatCompletionResult> createChatCompletion(@RequestBody ChatCompletionRequest request);

    // spring 6 以下
    // // 必須嚴格按照HTTPMethod + " " + apiEndpoint
    // @RequestLine("POST /v1/chat/completions")
    // Mono<ChatCompletionResult> createChatCompletion(@RequestBody ChatCompletionRequest request);

}

