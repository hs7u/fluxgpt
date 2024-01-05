package com.openai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeatherResponese {
    public String location;
    public int temperature;
    public WeatherEnum unit;
}
