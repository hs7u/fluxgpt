package com.openai.entity;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Table(name = "product_advice_record")
public class ProductAdviceRecord implements Serializable {
    
    @Id
    @Column("id")
    private long id;

    @Column("code")
    private String code;
    
    @Column("content")
    private String content;

    @Column("type")
    private String type;

    @Column("create_time")
    @CreatedDate // not working with native query
    private Instant createTime;
    
    @Column("update_time")
    @LastModifiedDate // not working with native query
    private Instant updateTime;
}
