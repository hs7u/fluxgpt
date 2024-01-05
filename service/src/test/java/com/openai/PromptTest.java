package com.openai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.openai.completion.ChatCompletionRequest;
import com.openai.completion.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.openai.constants.ModelEnum;
import com.openai.constants.ToolChoice;
import com.openai.entity.ProductAdviceRecord;
import com.openai.entity.AnalyzerResponse;
import com.openai.entity.Product;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.completion.ChatMessage;
import com.openai.completion.ChatMessageRole;
import com.openai.service.FunctionExecutor;
import com.openai.service.OpenAiService;
import com.openai.utils.TikTokensUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FluxGptApplication.class)
@EnableAutoConfiguration
public class PromptTest {
    
    static final ModelEnum MODEL = ModelEnum.GPT_3_5_TURBO_1106;
    static final int MAX_TOKENS = 800;
    static final String CHARACTER_ROLE = "希望由具有使用技術分析工具理解圖表的經驗的合格人員提供幫助，同時解釋世界範圍內普遍存在的宏觀經濟環境，從而幫助客戶獲得長期優勢需要明確的判斷，因此通過準確記錄的明智預測來尋求同樣的幫助！根據目前的情況告訴我們您對未來交易市場的看法";

    @Autowired
    OpenAiService service;

    @Value("${news1}")
    String GLOBAL_NEWS1;
    @Value("${news2}")
    String GLOBAL_NEWS2;
    @Value("${news3}")
    String GLOBAL_NEWS3;
    @Value("${news4}")
    String GLOBAL_NEWS4;
    @Value("${news5}")
    String GLOBAL_NEWS5;
    @Value("${news6}")
    String GLOBAL_NEWS6;
    @Value("${news7}")
    String GLOBAL_NEWS7;

    // 應該是排程執行時從db撈取
    static final String prompt = "请提供<%s>最新的解读和分析，并在最后请你根据目前的数据，提供<%s>价格乐观、中性或是悲观的看法，排除中国敏感在意的关键字";
    static final String code = "XAUUSD"; 

    @Test
    public void functionCallTest() {
        final List<String> NEWS = Arrays.asList("GLOBAL_NEWS5");
        FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(ChatFunction.builder()
                .name("get_product_analyze")
                .description("Get opinion of current product")
                .executor(Product.class, w -> new AnalyzerResponse(w.code, w.unit, 1938, NEWS))
                .build()));

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM, CHARACTER_ROLE);
        messages.add(systemMessage);

        String query = String.format(prompt, code, code);
        log.info("First Query: {}", query);
        ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER, query);
        messages.add(firstMsg);
        
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(MODEL)
                .messages(messages)
                .functions(functionExecutor.getFunctions())
                .functionCall(ChatCompletionRequestFunctionCall.of(ToolChoice.AUTO))
                .n(1)
                .temperature(1.5d)
                .maxTokens(MAX_TOKENS)
                .logitBias(new HashMap<>())
                .build();

        long startTime = System.nanoTime();

        // # Step 1: send the conversation and available functions to the model
        CompletableFuture<ProductAdviceRecord> future = service.createChatCompletionFuture(chatCompletionRequest)
        .thenApplyAsync(completionResult -> {
            ChatMessage responseMessage = completionResult.getChoices().get(0).getMessage();
            messages.add(responseMessage); 
            ChatFunctionCall functionCall = responseMessage.getFunctionCall();
            // # Step 2: check if the model wanted to call a function
            if (functionCall != null) {
                log.info("Trying to execute {}{}", functionCall.getName(), "...");
                // # Step 3: call the function
                Optional<ChatMessage> message = functionExecutor.executeAndConvertToMessageSafely(functionCall);
                if (message.isPresent()) {
                    log.info("Executed {}{}", functionCall.getName(), ".");
                    messages.add(message.get());
                } else {
                    log.info("Something went wrong with the execution of {}{}", functionCall.getName(), "...");
                }
            }
            log.info("function token: {}", TikTokensUtil.tokens(MODEL, functionCall, functionExecutor.getFunctions()));
            return null;
        })
        .thenCompose(no_val -> { 
            chatCompletionRequest.setMessages(messages);
            // get a new response from the model where it can see the function response
            return service.createChatCompletionFuture(chatCompletionRequest);
        })  
        .thenApplyAsync(completionResult -> { 
            log.info("total token calc by util: {}", TikTokensUtil.tokens(MODEL, messages));
            log.info("total token by response: {}", completionResult.getUsage());
            return completionResult.getChoices().get(0).getMessage();
        })
        .thenCompose(responseMessage -> {
            String content = responseMessage.getContent();
            log.info("content: {}", content);
            return service.saveProductAdviceRecord(ProductAdviceRecord.builder().code(code).content(content).build());
        });

        ProductAdviceRecord data = future.join();
        log.info("insert date: {}", data);

        long elapsedTime = System.nanoTime() - startTime;
        log.info("process time: {}",  (double)elapsedTime / 1_000_000_000.0);
                                                    
    }
}
