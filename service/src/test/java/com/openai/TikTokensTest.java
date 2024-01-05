package com.openai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.completion.ChatMessage;
import com.openai.completion.ChatMessageRole;
import com.openai.constants.ModelEnum;
import com.openai.entity.AnalyzerResponse;
import com.openai.entity.Product;
import com.openai.service.FunctionExecutor;
import com.openai.utils.TikTokensUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FluxGptApplication.class)
@EnableAutoConfiguration
public class TikTokensTest {
    static final ModelEnum MODEL = ModelEnum.GPT_3_5_TURBO_16K_0613;
    // static final String CHARACTER_ROLE = "Want assistance provided by qualified individuals enabled with experience on understanding charts using technical analysis tools while interpreting macroeconomic environment prevailing across world consequently assisting customers acquire long term advantages requires clear verdicts therefore seeking same through informed predictions written down precisely! "
    //                                     + "Can you tell us your opinion of future trading market based upon current conditions?";
    static final String CHINESE_ROLE = "希望由具有使用技術分析工具理解圖表的經驗的合格人員提供幫助，同時解釋世界範圍內普遍存在的宏觀經濟環境，從而幫助客戶獲得長期優勢需要明確的判斷，因此通過準確記錄的明智預測來尋求同樣的幫助！您能否根據目前的情況告訴我們您對未來交易市場的看法?";

    static final String CHINESE_CONTENT = "根据最新的报导和分析，黄金（XAUUSD）的价格受到中东以巴地区的冲突和紧张局势的影响，引发了避险需求，使黄金价格接近2000美元的关键水平。自2023年以来，黄金价格已经上涨，涨幅超过了标普500指数。"
                            + "截至10月26日，黄金近月期货价格上涨了9.2%，而同期内，标普500指数上涨了7.8%。此外，中东以巴冲突可能扩散至产油地区，进一步加剧了避险情绪。但是，美元的强势和高债券收益率增加了持有黄金的机会成本，"
                            + "这意味着只要局势稍有缓和，例如以色列和哈马斯达成停火协议，黄金价格可能会再次下跌。另外，美元指数（DXY）创下自10月6日以来的新高，显示出美元的强势。最新公布的美国第三季度国内生产总值（GDP）年增率高达4.9%，"
                            + "引发了关于美联储可能收紧货币政策的担忧。由于GDP表现强劲，2024年可能不太可能出现降息，这也对市场产生了影响。";

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
    @Autowired
    ObjectMapper mapper;

    static final String query = "请提供<XAUUSD>最新的解读和分析，并在最后请你根据目前的数据，提供<XAUUSD>价格乐观、中性或是悲观的看法，排除中国敏感在意的关键字";

    @Test
    public void main() {
        final List<String> NEWS = Arrays.asList(GLOBAL_NEWS1,GLOBAL_NEWS2,GLOBAL_NEWS3,GLOBAL_NEWS4,GLOBAL_NEWS5,GLOBAL_NEWS6);
        try {
            String name = "get_product_analyze";
            String arguments = "{\"code\":\"XAUUSD\",\"unit\":\"NEUTRAL\"}";
            FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(ChatFunction.builder()
                    .name(name)
                    .description("Get opinion of current product")
                    .executor(Product.class, w -> new AnalyzerResponse(w.code, w.unit, 2022, NEWS))
                    .build()));


            List<ChatMessage> messages = new ArrayList<>();
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM, CHINESE_ROLE);
            messages.add(systemMessage);

            ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER, query);
            messages.add(firstMsg);
            
            /****************************************************************/
            ChatMessage firstRequest = new ChatMessage();
            ChatFunctionCall functionCall = new ChatFunctionCall();
            functionCall.setName(name);
            JsonNode actualObj = mapper.readTree(arguments);
            functionCall.setArguments(actualObj);
            
            firstRequest.setFunctionCall(functionCall);
            firstRequest.setRole(ChatMessageRole.ASSISTANT);
            
            messages.add(firstRequest);

            Optional<ChatMessage> message = functionExecutor.executeAndConvertToMessageSafely(functionCall);
            messages.add(message.get());

            /****************************************************************/

            int requestToken = TikTokensUtil.tokens(MODEL, messages);
            int functionCallToken = TikTokensUtil.tokens(MODEL, functionCall, functionExecutor.getFunctions());
            log.info("base request call token: {}", requestToken);
            log.info("function call token: {}", functionCallToken);
            log.info("request token: {}", requestToken + functionCallToken);

            /****************************************************************/
            ChatMessage secondRequest = new ChatMessage();
            secondRequest.setContent(CHINESE_CONTENT);
            secondRequest.setFunctionCall(functionCall);
            secondRequest.setRole(ChatMessageRole.ASSISTANT);

            messages.add(secondRequest);

            int responsetoken = TikTokensUtil.tokens(MODEL, messages);
            log.info("response token: {}", responsetoken);
           
            
            log.info("total token: {}", requestToken + functionCallToken + responsetoken);
            /****************************************************************/

            // for(String news : NEWS) {
            //     log.info("chineseNews letgth: {} token: {}",  news.length(),  TikTokensUtil.tokens(MODEL, news));
            // }
            // log.info("globaNews letgth: {} globalNewsToken token: {}", GLOBAL_NEWS.length(), globalNewsToken);
            // int contentToken = TikTokensUtil.tokens(MODEL.getName(), CHINESE_CONTENT);
            // log.info("contentToken token: {}", contentToken);

            log.info("arguments: {}", functionExecutor.getFunctions().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
