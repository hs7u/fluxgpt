package com.openai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openai.OpenAiApi;
import com.openai.completion.ChatCompletionRequest;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.config.mixin.ChatCompletionRequestMixIn;
import com.openai.config.mixin.ChatFunctionCallMixIn;
import com.openai.config.mixin.ChatFunctionMixIn;

// import reactivefeign.webclient.WebReactiveFeign;
// import reactivefeign.webclient.WebReactiveOptions;

@Configuration
public class BeanConfig {
    
    @Value("${openai.baseUrl}")
    private String baseUrl;
    @Value("${openai.token}")
    private String token;

    @Bean
	TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(10);
		taskExecutor.setMaxPoolSize(50);
		return taskExecutor;
	}

    @Bean
    ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.addMixIn(ChatFunction.class, ChatFunctionMixIn.class);
        mapper.addMixIn(ChatCompletionRequest.class, ChatCompletionRequestMixIn.class);
        mapper.addMixIn(ChatFunctionCall.class, ChatFunctionCallMixIn.class);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    ExchangeStrategies getExchangeStrategies(ObjectMapper mapper) {
        return ExchangeStrategies
            .builder()
            .codecs(clientDefaultCodecsConfigurer -> {
                clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(100*1024*1024);
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
            }).build();
    }

    @Bean
    WebClient buildWebClient(ExchangeStrategies exchangeStrategies) {
        return WebClient.builder()
                        .baseUrl(baseUrl)
                        .exchangeStrategies(exchangeStrategies)
                        .defaultHeaders(httpHeaders -> {
                            httpHeaders.setBearerAuth(token);
                        })
                        .build();
    }

    @Bean
    HttpServiceProxyFactory proxyFactory(WebClient webClient) {
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();
    }

    @Bean
    OpenAiApi apiClient(HttpServiceProxyFactory factory) {
        return factory.createClient(OpenAiApi.class);
    }

    // spring 6 以下使用
    // @Bean
    // OpenAiApi openAiApi(ExchangeStrategies exchangeStrategies) {
    //     return WebReactiveFeign
    //             .<OpenAiApi>builder(
    //                 WebClient.builder()
    //                         .exchangeStrategies(exchangeStrategies)
    //                         .defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(token))
    //             )
    //             .options(new WebReactiveOptions.Builder().setConnectTimeoutMillis(connectTimeoutMillis).build())
    //             .target(OpenAiApi.class, baseUrl);
    // }

}
