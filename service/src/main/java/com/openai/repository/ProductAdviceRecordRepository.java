package com.openai.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.openai.entity.ProductAdviceRecord;

/**
 * 只有使用curd所以不繼承擴展後的 R2dbcRepository
 */
@Repository
public interface ProductAdviceRecordRepository extends ReactiveCrudRepository<ProductAdviceRecord, Long> {
    
}