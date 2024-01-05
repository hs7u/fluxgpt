package com.openai.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.openai.completion.ChatFunction;
import com.openai.completion.ChatFunctionCall;
import com.openai.completion.ChatMessage;
import com.openai.completion.ChatToolCall;
import com.openai.constants.ModelEnum;
import com.openai.constants.ModelList;

/**
 * Token calculation
 */
public class TikTokensUtil {
     /**
     * Model name corresponds to Encoding
     */
    private static final Map<String, Encoding> modelMap = new HashMap<>();
    /**
     * Registry instance
     */
    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonSchemaConfig config = JsonSchemaConfig.vanillaJsonSchemaDraft4();
    private static final JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(mapper, config);

    static {
        for (ModelType modelType : ModelType.values()) {
            modelMap.put(modelType.getName(), registry.getEncodingForModel(modelType));
        }
        //加入新版本model
        for (ModelEnum modelEnum : ModelEnum.values()) {
            String modelName = modelEnum.getName();
            if (modelName.startsWith("gpt-3.5")) {
                modelMap.put(modelName, registry.getEncodingForModel(ModelType.GPT_3_5_TURBO));
            } else {
                modelMap.put(modelName, registry.getEncodingForModel(ModelType.GPT_4));
            }
        }
    }

    /**
     * Get encoding array through Encoding and text.
     *
     * @param enc  Encoding type
     * @param text Text information
     * @return Encoding array
     */
    public static List<Integer> encode(Encoding enc, String text) {
        return isBlank(text) ? new ArrayList<>() : enc.encode(text);
    }

    /**
     * Get the encoded array by model name using encode.
     *
     * @param text Text information
     * @return Encoding array
     */
    public static List<Integer> encode(String modelName, String text) {
        if (isBlank(text)) {
            return new ArrayList<>();
        }
        Encoding enc = getEncoding(modelName);
        if (Objects.isNull(enc)) {
            return new ArrayList<>();
        }
        List<Integer> encoded = enc.encode(text);
        return encoded;
    }

    /**
     * Get an Encoding object by model name.
     *
     * @param modelName
     * @return Encoding
     */
    public static Encoding getEncoding(String modelName) {
        return modelMap.get(modelName);
    }

    /**
     * Calculate tokens of text information through Encoding.
     *
     * @param enc  Encoding type
     * @param text Text information
     * @return Number of tokens
     */
    public static int tokens(Encoding enc, String text) {
        return encode(enc, text).size();
    }

    /**
     * Calculate the tokens of a specified string by model name.
     *
     * @param modelName
     * @param text
     * @return Number of tokens
     */
    public static int tokens(String modelName, String text) {
        return encode(modelName, text).size();
    }

    /**
     * Calculate the tokens of a specified string by model enum.
     * 
     * @param modelEnum
     * @param text
     * @return
     */
    public static int tokens(ModelEnum modelEnum, String text) {
        return encode(getEncoding(modelEnum.getName()), text).size();
    }

    /**
     * Calculate logic:
     * <a href=https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb>link</a>
     *
     * @param modelName
     * @param messages
     * @return Number of tokens
     */
    public static int tokens(ModelEnum model, List<ChatMessage> messages) {
        Encoding encoding = getEncoding(model.getName());
        int tokensPerMessage = 0;
        int tokensPerName = 0;
      
        if(ModelList.MODELS.contains(model)) {
            tokensPerMessage = 3;
            tokensPerName = 1;
        } else {
            tokensPerMessage = 4;
            tokensPerName = -1;
        }

        int sum = 0;
        for (ChatMessage msg : messages) {
            sum += tokensPerMessage;
            sum += tokens(encoding, msg.getContent());
            sum += tokens(encoding, msg.getRole().value());
            sum += tokens(encoding, msg.getName());
            if (isNotBlank(msg.getName())) {
                sum += tokensPerName;
            }
        }
        sum += 3;
        return sum;
    }

    /**
     * magic number
     * 12 is the tokens for the functions frame: functions": [ { "name": "", "description": "" ...
     * 11 is the tokens for parameters frame: "parameters": { "type": "object", "properties": {}, "required": [] }
     * @param model
     * @param functionCall
     * @param functions
     * @return
     */
    public static int tokens(ModelEnum model, ChatToolCall toolCalls, List<ChatFunction> functions) {
        return tokens(model, toolCalls.getFunction(), functions);
    }
    public static int tokens(ModelEnum model, ChatFunctionCall functionCall, List<ChatFunction> functions) {
        Encoding encoding = getEncoding(model.getName());
        AtomicInteger sum = new AtomicInteger(0);
        if (functionCall != null) {
            sum.addAndGet(tokens(encoding, functionCall.getArguments().asText()));
        }
        
        for (ChatFunction function : functions) {
            sum.addAndGet(tokens(encoding, function.getName()));
            sum.addAndGet(tokens(encoding, function.getDescription()));
            if (function.getParametersClass() != null) {
                JsonNode node = jsonSchemaGenerator.generateJsonSchema(function.getParametersClass());
                if (node.has("properties")) {
                    node.get("properties").fieldNames().forEachRemaining(propertiesKey -> {
                        sum.addAndGet(tokens(encoding, propertiesKey));
                        JsonNode subNode = node.get("properties").get(propertiesKey);
                        subNode.fields().forEachRemaining(field -> {
                            String key = field.getKey();
                            switch (key) {
                                case "type":
                                case "description": {
                                    sum.addAndGet(2);
                                    sum.addAndGet(tokens(encoding, subNode.get(key).asText()));
                                }
                                    break;
                                case "enum": {
                                    sum.addAndGet(-3);
                                    for (Object o : subNode.get(field.getKey())) {
                                        sum.addAndGet(3);
                                        sum.addAndGet(tokens(encoding, o.toString()));
                                    }
                                }
                                    break;
                                default:
                                    // log.warn("not supported field {}", field);
                                    break;
                            }
                        });
                   });
                   
                }
                sum.addAndGet(11);
            }
        }
        sum.addAndGet(12);

        return sum.get();
    }

    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 65279 || c == 8234 || c == 0 || c == 12644 || c == 10240 || c == 6158;
    }

    public static boolean isBlankChar(char c) {
        return isBlankChar((int) c);
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    public static boolean isBlank(CharSequence str) {
        int length;
        if (str != null && (length = str.length()) != 0) {
            for (int i = 0; i < length; ++i) {
                if (!isBlankChar(str.charAt(i))) {
                    return false;
                }
            }
        } 
        return true;
    }
}
