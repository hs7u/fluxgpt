package com.openai.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.openai.OpenAiApi;
import com.openai.OpenAiError;
import com.openai.completion.ChatCompletionRequest;
import com.openai.completion.ChatCompletionResult;
import com.openai.entity.ProductAdviceRecord;
import com.openai.repository.ProductAdviceRecordRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Update.update;

@Slf4j
@Service
public class OpenAiService {

    @Autowired
    private OpenAiApi api;
    @Autowired
    private ProductAdviceRecordRepository repo;
    @Autowired
    private R2dbcEntityTemplate template;

    public Flux<ProductAdviceRecord> findAll() {
        return repo.findAll();
    }
    
    @Async
    public CompletableFuture<ProductAdviceRecord> saveProductAdviceRecord(ProductAdviceRecord record) {
        return saveProductAdviceRecordMono(record).toFuture();
    }
    @Transactional
    public Mono<ProductAdviceRecord> saveProductAdviceRecordMono(ProductAdviceRecord record) {
        return repo.save(record)
                    .doOnError(Exception.class, throwable -> {
                        log.error("repo save failed throwable: {}", throwable);
                    });
    }
    
    @Async
    public CompletableFuture<Long> updateProductAdviceRecord(ProductAdviceRecord record) { 
        return updateProductAdviceRecordMono(record).toFuture();
    }
    @Transactional
    public Mono<Long> updateProductAdviceRecordMono(ProductAdviceRecord record) { 
        return template.update(query(where("code").is(record.getCode())), update("content", record.getContent()), ProductAdviceRecord.class)
                        .doOnError(Exception.class, throwable -> {
                            log.error("repo update failed throwable: {}", throwable);
                        });
    }
    
    public ChatCompletionResult createChatCompletion(ChatCompletionRequest request) {
        return execute(api.createChatCompletion(request));
    }

    /**
     * 因不確定調用openai響應的時間,使用completableFuture包裝調用,直接Mono.block()/Mono.subscribe()/Mono.toFuture().get()會造成阻塞
     * @param request
     * @return
     */
    @Async
    public CompletableFuture<ChatCompletionResult> createChatCompletionFuture(ChatCompletionRequest request) {
        return getCompletableFuture(api.createChatCompletion(request));
    }

    public Mono<ChatCompletionResult> createChatCompletionMono(ChatCompletionRequest request) {
        return getMono(api.createChatCompletion(request));
    }

    /**
     * is blocking method
     * Calls the Open AI api, returns the response, and parses error messages if the request fails
     */
    private <T> T execute(Mono<T> apiCall) {
        return getCompletableFuture(apiCall).join();
    }

    /**
     * @param mono
     * @return
     */
    private <T> CompletableFuture<T> getCompletableFuture(Mono<T> mono) {
        return getMono(mono).toFuture();
    }

    private <T> Mono<T> getMono(Mono<T> mono) {
        return mono.doOnError(WebClientResponseException.class, throwable -> {
            try {
                OpenAiError error = throwable.getResponseBodyAs(OpenAiError.class);
                log.error("mono.doOnError throwable: {}", error.toString());
            }  catch (Exception e) {
                // couldn't parse openai error
                e.printStackTrace();
            }
        });
    }

}
