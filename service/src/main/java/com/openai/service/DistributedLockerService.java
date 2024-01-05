package com.openai.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.redisson.api.LockOptions;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openai.service.retry.RetryThrowable;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class DistributedLockerService {

    @Autowired
    private RedissonReactiveClient reactiveClient;

    private static final String KEY = "reactive-lock";

    /**
     * none reactive resource can use doWithLock( Mono.defer(() -> "your method etc..." ) );
     * @param Mono<T> func
     * @param String key
     * @return
     */
    public <T> Mono<T> doWithLockAndSpecificKey(Mono<T> func, String key) { 
        return doWithLock(null, KEY);
    }
    public <T> Mono<T> doWithLock(Mono<T> func) { 
        return doWithLock(null, KEY);
    }
    private <T> Mono<T> doWithLock(Mono<T> func, String key) {
        long thread = Thread.currentThread().getId();
        RLockReactive rlock = reactiveClient.getSpinLock(KEY, LockOptions.defaults());
        
        return Mono.usingWhen(
                    Mono.defer(() -> rlock.tryLock(1000, 1000, TimeUnit.MILLISECONDS, thread)), 
                    hasLock -> {
                        if (hasLock) {
                            log.info("thread: {} has lock", thread);
                            return func.doOnSuccess(s -> rlock.unlock(thread));
                        }
                        else { 
                            log.info("thread: {} waiting lock", thread);
                            return Mono.error(new RetryThrowable("No lock acquired"));
                        }
                    },
                    hasLock -> {
                        log.info("thread: {} {}", thread, hasLock ? "job done unlock" : "cleanup");
                        return hasLock ? rlock.unlock(thread) : Mono.empty();
                    }
                )
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(100)).jitter(0.5d).filter(throwable -> RetryThrowable.class.isInstance(throwable)));
    }
}
