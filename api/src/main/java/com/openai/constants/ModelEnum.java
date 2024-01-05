package com.openai.constants;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelEnum {
    /**
     * 要注意openai 是否有更新
     * 會影響token計算跟使用
     */
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k"),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106"),
    @Deprecated
    GPT_3_5_TURBO_16K_0613("gpt-3.5-turbo-16k-0613"),
    GPT_4("gpt-4"),
    GPT_4_32K("gpt-4-32k"),
    GPT_4_1106_PREVIEW("gpt-4-1106-preview"),
    GPT_4_0613("gpt-4-0613"),
    GPT_4_32K_0613("gpt-4-32k-0613"),
    ;

    @JsonValue
    private String name;
}
