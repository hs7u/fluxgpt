package com.openai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AiParam {

    @JsonProperty("model")
    private String model;
    @JsonProperty("publish")
    private PublishSetting publish;
    @JsonProperty("fetchRecord")
    private FetchRecordSetting fetchRecord;

    public Builder builder() {
        return new Builder();
    }

    private AiParam(Builder builder) {
        this.publish = builder.publish;
        this.fetchRecord = builder.fetchRecord;
    }

    @AllArgsConstructor
    @Getter
    private static enum PublishSetting {
        DISABLE(0),
        ENABLE(1);

        @JsonValue
        private Integer code;
    }

    @AllArgsConstructor
    @Getter
    private static enum FetchRecordSetting {
        DISABLE(0),
        ENABLE(1);

        @JsonValue
        private Integer code;
    }

    public static class Builder {
        PublishSetting publish;
        FetchRecordSetting fetchRecord;

        public Builder setPublishEnable() {
            this.publish = PublishSetting.ENABLE;
            return this;
        }

        public Builder setPublishDiable() {
            this.publish = PublishSetting.DISABLE;
            return this;
        }

        public Builder setFetchRecordEnable() {
            this.fetchRecord = FetchRecordSetting.ENABLE;
            return this;
        }

        public Builder setFetchRecordDiable() {
            this.fetchRecord = FetchRecordSetting.DISABLE;
            return this;
        }

        public AiParam build() {
            return new AiParam(this);
        }
    }
}
