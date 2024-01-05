package com.openai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import reactor.core.publisher.Mono;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FluxGptApplication.class)
@EnableAutoConfiguration
public class FunctionCallSample {
    
    static final ModelEnum MODEL = ModelEnum.GPT_3_5_TURBO_1106;
    static final int MAX_TOKENS = 550;
    // static final String CHARACTER_ROLE = "Want assistance provided by qualified individuals enabled with experience on understanding charts using technical analysis tools while interpreting macroeconomic environment prevailing across world consequently assisting customers acquire long term advantages requires clear verdicts therefore seeking same through informed predictions written down precisely! "
    //                                     + "Can you tell us your opinion of future trading market based upon current conditions?";
    static final String CHARACTER_ROLE = "希望由具有使用技術分析工具理解圖表的經驗的合格人員提供幫助，同時解釋世界範圍內普遍存在的宏觀經濟環境，從而幫助客戶獲得長期優勢需要明確的判斷，因此通過準確記錄的明智預測來尋求同樣的幫助！您能否根據目前的情況告訴我們您對未來交易市場的看法?";

    // static final String GLOBAL_NEWS = "The latest report indicates that the conflicts and tensions in the Middle East, particularly in the Israel-Palestine region, have sparked a surge in demand for safe-haven assets, propelling gold prices towards the significant threshold of $2000. Since 2023, the price of gold has risen, surpassing the growth seen in the S&P 500 index."
    //                             + "According to statistics from Dow Jones Market Data, from January 1st to October 26th, the S&P 500 index increased by 7.8%. Meanwhile, within the same period, the price of near-month gold futures surged by 9.2%. Data from FactSet reveals that as of October 26th, October gold futures climbed by 0.16%, reaching $1987.20, marking a new high since May 16th. Since the beginning of October, gold prices have surged by over 7.5%."
    //                             + "Additionally, BCA Research has highlighted the potential for the conflict in the Middle East, particularly in Israel-Palestine, to spread to oil-producing regions such as Iraq, the Persian Gulf, and Iran, further intensifying risk aversion sentiment."
    //                             + "In contrast, the S&P 500 index dropped by 3.5% in October, continuing the downtrend observed in the preceding two months. Should the S&P 500 decline in October, it would mark the first consecutive three-month decline since March 2020."
    //                             + "Gold's performance in 2022 and 2020 has outpaced that of US stocks. Some experts suggest that the Middle East conflict is a primary factor driving the increase in gold prices."
    //                             + "However, market analysts point out that the strength of the US dollar and high bond yields have increased the opportunity cost of holding gold. This implies that any easing of tensions, such as the potential agreement between Israel and Hamas for a ceasefire, could result in another decline in gold prices."
    //                             + "Furthermore, the US Dollar Index (DXY) hit a new high since October 6th, signaling the continued strength of the US dollar."
    //                             + "The recently disclosed annualized Gross Domestic Product (GDP) growth rate for the third quarter of the United States reached a remarkable 4.9%, achieving a new high in nearly two years. This has sparked concerns regarding the Federal Reserve's potential implementation of tighter monetary policies. Greg Bassuk, CEO of AXS Investments, suggested that due to the robust GDP performance, the possibility of a rate cut in 2024 might be less likely, impacting the market dynamics.";
    

    static final String GLOBAL_NEWS = "最新報導指出，中東以巴地區的爭端和衝突已經引發了避險需求，激勵了黃金價格，使其趨近於2000美元的大關。自2023年以來，黃金價格已上漲，漲幅超過了標準普爾500指數。"
                                + "據Dow Jones Market Data統計，自1月1日至10月26日，標普500指數上漲了7.8%，而同期內，黃金近月期貨價格上漲了9.2%。FactSet資料顯示，截至10月26日，10月期貨黃金價格上漲0.16%，達到1987.20美元，創下自5月16日以來的新高，而自10月初以來，黃金價格已上漲超過7.5%。"
                                + "此外，BCA Research指出，中東以巴衝突可能擴散至產油地區，如伊拉克、波斯灣和伊朗，進一步加劇了避險情緒。"
                                + "相反，標普500指數在10月份下跌了3.5%，延續了前兩個月的下跌趨勢。如果標普500在10月份下跌，這將是自2020年3月以來首次連續三個月的下跌。"
                                + "黃金在2022年和2020年的表現也超越了美股。一些專家指出，中東衝突是黃金價格上漲的主要原因之一。"
                                + "然而，市場分析師指出，美元的強勢和高債券殖利率使得持有黃金的機會成本升高，這意味著只要情勢稍有緩和，例如以色列和哈馬斯達成停火協議，黃金價格可能再次下跌。"
                                + "另外，美元指數（DXY）也創下自10月6日以來的新高，這表明美元的強勢。"
                                + "最新公布的美國第三季國內生產毛額（GDP）年增率高達4.9%，創下近2年來的新高，引發了有關聯邦儲備系統（Fed）可能實行緊縮貨幣政策的擔憂。AXS Investments執行長Greg Bassuk指出，由於GDP表現強勁，2024年可能不太可能出現降息，這也對市場產生了影響。";

    // 應該是排程執行時從db撈取
    static final String prompt = "请提供<%s>最新的解读和分析，并在最后请你根据目前的数据，提供<%s>价格乐观、中性或是悲观的看法，排除中国敏感在意的关键字";
    static final String code = "XAUUSD"; 
    
    @Autowired
    OpenAiService service;

    @Test
    public void functionCallTest() {

        FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(ChatFunction.builder()
                .name("get_product_analyze")
                .description("Get opinion of current product")
                .executor(Product.class, w -> new AnalyzerResponse(w.code, w.unit, 1030, Collections.singletonList(GLOBAL_NEWS)))
                .build()));

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM, CHARACTER_ROLE);
        messages.add(systemMessage);

        // String query = "What do you think of the XAUUSD?";
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
                .maxTokens(MAX_TOKENS)
                .logitBias(new HashMap<>())
                .build();

        long startTime = System.nanoTime();
        // 同步
        // try {
        //     ChatMessage responseMessage = null;
        //     ChatCompletionResult completionResult = null;
        //     ChatFunctionCall forCountToken = null;
        //     // # Step 1: send the conversation and available functions to the model
        //     completionResult = service.createChatCompletion(chatCompletionRequest);
        //     responseMessage = completionResult.getChoices().get(0).getMessage();
        //     messages.add(responseMessage); 
        //     ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        //     // # Step 2: check if the model wanted to call a function
        //     if (functionCall != null) {
        //         log.info("Trying to execute {}{}", functionCall.getName(), "...");
        //         // # Step 3: call the function
        //         Optional<ChatMessage> message = functionExecutor.executeAndConvertToMessageSafely(functionCall);
        //         if (message.isPresent()) {
        //             log.info("Executed {}{}", functionCall.getName(), ".");
        //             messages.add(message.get());
        //         } else {
        //             log.info("Something went wrong with the execution of {}{}", functionCall.getName(), "...");
        //         }
        //     }
        //     // get a new response from the model where it can see the function response
        //     completionResult = service.createChatCompletion(chatCompletionRequest);
        //     responseMessage = completionResult.getChoices().get(0).getMessage();
        //     // service.mergeProductAdviceRecord(ProductAdviceRecord.builder().code(code).content(responseMessage.getContent()).build());

        //     log.info("function token: {}", TikTokensUtil.tokens(MODEL, forCountToken, functionExecutor.getFunctions()));
        //     log.info("function token: {}", TikTokensUtil.tokens(MODEL, functionCall, functionExecutor.getFunctions()));
        //     log.info("total token calc by util: {}", TikTokensUtil.tokens(MODEL, messages));
        //     log.info("total token by response: {}", completionResult.getUsage());
        //     log.info("Response: {}", responseMessage.getContent());
        // } catch (NullPointerException e) {
        //     // completableFuture get null
        //     e.printStackTrace();
        // }
            
        /************************************************************************************************************ */
        // 非同步
        // # Step 1: send the conversation and available functions to the model
        // CompletableFuture<Long> future = service.createChatCompletionFuture(chatCompletionRequest)
        // .thenApplyAsync(completionResult -> {
        //     ChatMessage responseMessage = completionResult.getChoices().get(0).getMessage();
        //     messages.add(responseMessage); 
        //     ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        //     // # Step 2: check if the model wanted to call a function
        //     if (functionCall != null) {
        //         log.info("Trying to execute {}{}", functionCall.getName(), "...");
        //         // # Step 3: call the function
        //         Optional<ChatMessage> message = functionExecutor.executeAndConvertToMessageSafely(functionCall);
        //         if (message.isPresent()) {
        //             log.info("Executed {}{}", functionCall.getName(), ".");
        //             messages.add(message.get());
        //         } else {
        //             log.info("Something went wrong with the execution of {}{}", functionCall.getName(), "...");
        //         }
        //     }
        //     log.info("function token: {}", TikTokensUtil.tokens(MODEL, functionCall, functionExecutor.getFunctions()));
        //     return null;
        // })
        // .thenCompose(no_val -> service.createChatCompletionFuture(chatCompletionRequest))  // get a new response from the model where it can see the function response
        // .thenApplyAsync(completionResult -> { 
        //     log.info("total token calc by util: {}", TikTokensUtil.tokens(MODEL, messages));
        //     log.info("total token by response: {}", completionResult.getUsage());
        //     return completionResult.getChoices().get(0).getMessage();
        // })
        // .thenCompose(responseMessage -> 
        //     service.mergeProductAdviceRecord(ProductAdviceRecord.builder().code(code).content(responseMessage.getContent()).build())
        // );

        // Long data = future.join();
        // log.info("insert date: {}", data);

        /************************************************************************************************************ */
        // 非同步反應式
        Mono<ProductAdviceRecord> completionMono = service.createChatCompletionMono(chatCompletionRequest)
        .flatMap(completionResult -> {
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
            return service.createChatCompletionMono(chatCompletionRequest);
        })
        .flatMap(completionResult -> { 
            log.info("total token calc by util: {}", TikTokensUtil.tokens(MODEL, messages));
            log.info("total token by response: {}", completionResult.getUsage());
            return Mono.just(completionResult.getChoices().get(0).getMessage());
        })
        .flatMap(responseMessage ->
            service.saveProductAdviceRecordMono(ProductAdviceRecord.builder().code(code).content(responseMessage.getContent()).build())
        );

        // completionMono.subscribe(data -> log.info("insert date: {}", data));
        // 這裡使用block式為阻止junit主線程先結束
        log.info("insert date: {}", completionMono.block());

        long elapsedTime = System.nanoTime() - startTime;
        log.info("process time: {}",  (double)elapsedTime / 1_000_000_000.0);
                                                    
    }
}
