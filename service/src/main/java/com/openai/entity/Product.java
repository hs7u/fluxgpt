package com.openai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;

/**
 * 發出詢問的paramClass 結構參考
 * <a>https://platform.openai.com/docs/guides/gpt/function-calling<a>
 */
@Data
public class Product {
    @JsonPropertyDescription("product code, for example: XASUUSD")
    public String code;

    @JsonPropertyDescription("The option unit, can be 'OPTIMISTIC' or 'PESSIMISTIC' or 'NEUTRAL' ")
    @JsonProperty(required = true)
    public OptionUnit unit;
}