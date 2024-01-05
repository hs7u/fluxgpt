package com.openai.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;

import com.openai.NullCallException;
import com.openai.completion.ChatCompletionRequest;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.completion.ChatMessage;
import com.openai.completion.ChatMessageRole;
import com.openai.completion.ChatToolCall;
import com.openai.constants.ModelEnum;
import com.openai.constants.ToolChoice;
import com.openai.entity.AnalyzerResponse;
import com.openai.entity.Product;
import com.openai.entity.Weather;
import com.openai.entity.WeatherEnum;
import com.openai.entity.WeatherResponese;
import com.openai.service.DistributedLockerService;
import com.openai.service.FunctionExecutor;
import com.openai.service.OpenAiService;
import com.openai.utils.TikTokensUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
// @RestController
public class APITestController {
    
    static final ModelEnum MODEL = ModelEnum.GPT_3_5_TURBO_1106;
    static final int MAX_TOKENS = 1500;
    static final String CHARACTER_ROLE = "希望由具有使用技術分析工具理解圖表的經驗的合格人員提供幫助，同時解釋世界範圍內普遍存在的宏觀經濟環境，從而幫助客戶獲得長期優勢需要明確的判斷，因此通過準確記錄的明智預測來尋求同樣的幫助！根據目前的情況告訴我們您對未來交易市場的看法";

    @Autowired
    private OpenAiService aiService;
    @Autowired
    private DistributedLockerService lockService;

    @Value("${news7}")
    private String GLOBAL_NEWS1;

    // 應該是排程執行時從db撈取
    static final String prompt = "请提供<%s>最新的解读和至少500字的分析，并在最后请你根据目前的数据，提供<%s>价格乐观、中性或是悲观的看法，排除中国敏感在意的关键字";
    static final String code = "XAUUSD";
    // use different prompt test functionCall
    static final String prompt2 = "What's the weather like in Tokyo ?";
    static final String prompt3 = "What's the recipe of Pad Thai ?";

    // @GetMapping("chat")
    public Mono<ChatMessage> chat() {

        List<ChatMessage> messages = new ArrayList<>();
        // add gpt rule first to context
        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM, CHARACTER_ROLE);
        messages.add(systemMessage);

        String query = String.format(prompt, code, code);
        log.info("First Query: {}", query);
        // create message by your rule and your prompt just like normal
        ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER, query + "\n" +GLOBAL_NEWS1);
        messages.add(firstMsg);
        
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(MODEL)
                .messages(messages)
                .n(1)
                .maxTokens(MAX_TOKENS)
                .logitBias(new HashMap<>())
                .build();

        log.info("first: {}", chatCompletionRequest);

        Mono<ChatMessage> completionMono = aiService.createChatCompletionMono(chatCompletionRequest)
        .flatMap(completionResult -> Mono.just(completionResult.getChoices().get(0).getMessage()));
      
        return completionMono;
    }

    // @GetMapping("functionCall/{prompt}")
    public Mono<ChatMessage> functionCall(@PathVariable("prompt") String customPrompt) {

        // defined your function here like: jpa findNesByProductCode(String code) etc...; param came from requestClass
        // to make gpt generate  response with
        // and it will execute by functionExecutor
        ChatFunction productAnalyze = ChatFunction.builder()
                .name("get_product_analyze")
                .description("Get opinion of current product")
                .executor(Product.class, (p) -> new AnalyzerResponse(p.code, p.unit, mockGetPrice(p.code), mockGetNews(p.code)))
                .build();
        
        // make gpt answer temperature always use CELSIUS
        ChatFunction currentWeather = ChatFunction.builder()
                .name("get_current_weather")
                .description("Get the current weather")
                .executor(Weather.class, (w) -> new WeatherResponese(w.location, mockGetCurrentWeather(w.location), WeatherEnum.CELSIUS))
                .build();

        FunctionExecutor functionExecutor = new FunctionExecutor(Arrays.asList(productAnalyze, currentWeather));

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM, CHARACTER_ROLE);
        messages.add(systemMessage);

        ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER, customPrompt);
        messages.add(firstMsg);
        
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(MODEL)
                .messages(messages)
                .tools(functionExecutor.getTools())
                .toolChoice(ToolChoice.AUTO)
                .n(1)
                .maxTokens(MAX_TOKENS)
                .logitBias(new HashMap<>())
                .build();

        Mono<ChatMessage> completionMono = aiService.createChatCompletionMono(chatCompletionRequest)
        .flatMap(completionResult -> {
            ChatMessage responseMessage = completionResult.getChoices().get(0).getMessage();
            messages.add(responseMessage); 
            List<ChatToolCall> toolCalls = responseMessage.getToolCalls();

            // the prompt diden't trigger any function
            if(CollectionUtils.isEmpty(toolCalls)) {
                log.info("responseMessage : {}" , responseMessage);
                return Mono.error(new NullCallException(responseMessage));
            }

            // invoking multiple function calls in one response
            for(ChatToolCall call : toolCalls) {
                // # Step 2: check if the model wanted to call a function
                if (call != null) {
                    ChatFunctionCall functionCall = call.getFunction();
                    log.info("Trying to execute {}{}", functionCall.getName(), "...");
                    // # Step 3: call the function
                    Optional<ChatMessage> message = functionExecutor.executeAndConvertToMessageSafely(call);
                    if (message.isPresent()) {
                        log.info("Executed {}{}", functionCall.getName(), ".");
                        messages.add(message.get());
                    } else {
                        log.info("Something went wrong with the execution of {}{}", functionCall.getName(), "...");
                    }
                    log.info("function token: {}", TikTokensUtil.tokens(MODEL, functionCall, functionExecutor.getFunctions()));
                }
            }
            
            chatCompletionRequest.setMessages(messages);
            return aiService.createChatCompletionMono(chatCompletionRequest);
        })
        .flatMap(completionResult -> {
            log.info("total token calc by util: {}", TikTokensUtil.tokens(MODEL, messages));
            log.info("total token by response: {}", completionResult.getUsage());
            return Mono.just(completionResult.getChoices().get(0).getMessage());
        })
        // .flatMap(responseMessage -> {
        //     return lockService.doWithLock(aiService.mergeAnalyzerRecordMono(AnalyzerRecord.builder().code(code).content(responseMessage.getContent()).build()));
        // });
        .onErrorResume(NullCallException.class, e -> Mono.just(e.getChatMessage()));

        return completionMono;
    }

    
    private int mockGetCurrentWeather(String location) {
        if ("tokyo".equalsIgnoreCase(location))
            return 20;
        else 
            return 40;
    }

    private List<String> mockGetNews(String code) {
        if("XAUUSD".equalsIgnoreCase(code))
            return Arrays.asList(GLOBAL_NEWS1);
        else
            return Arrays.asList("no news");
    }

    private int mockGetPrice(String code) {
        if("XAUUSD".equalsIgnoreCase(code))
            return 2000;
        else
            return 0;
    }
}
