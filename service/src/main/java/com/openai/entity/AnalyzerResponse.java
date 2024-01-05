package com.openai.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyzerResponse {
    public String code;
    public OptionUnit unit;
    public int currentPrice;
    public List<String> news;
}