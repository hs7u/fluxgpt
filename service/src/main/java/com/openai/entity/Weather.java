package com.openai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;

@Data
public class Weather {

    @JsonPropertyDescription("get weather from this location")
    @JsonProperty(required = true)
    public String location;

    public int temperature;

    @JsonPropertyDescription("temperature format")
    @JsonProperty(required = true)
    public WeatherEnum unit;
}
