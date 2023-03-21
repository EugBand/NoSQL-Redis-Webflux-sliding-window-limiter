package com.epam.jmp.redislab.service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component("redisson")
@Primary
@RequiredArgsConstructor
public class RedissonRateLimitService implements RateLimitService {

    @Autowired
    Set<RateLimitRule> rateLimitRules;
    @Autowired
    RateLimitRule defaultRule;

    @Autowired
    ProxyManager<String> proxyManager;



    @Override
    public Mono<Boolean> shouldLimit(Set<RequestDescriptor> requestDescriptors) {

        return Flux.fromIterable(requestDescriptors)
                .map(this::getAccount)
                .map(rule -> {
                    int duration = getDuration(rule);
                    long curTime = System.currentTimeMillis() / 1000;
                    long curWindowKey = curTime / duration * duration;
                    AsyncBucketProxy bucket = resolveBucket(curWindowKey, rule);
                    long preWindowKey = curWindowKey - duration;
                    log.info("curWindowKey = {} ; preWindowKey = {}", curWindowKey, preWindowKey);
                    Long allowedRequest = Long.valueOf(rule.getAllowedNumberOfRequests());
                    AsyncBucketProxy preBucket = resolveBucket(preWindowKey, rule);
                    return preBucket.getAvailableTokens().thenCombine(bucket.getAvailableTokens(),
                            (r1, r2) -> {
                                log.info("preCount = {}; curCount = {}", r1, r2);
                                if (Objects.equals(r1, allowedRequest) && Objects.equals(r2, allowedRequest)) {
                                    return true;
                                }
                                double preWeight = (curTime - curWindowKey);
                                log.info("preWeight = {}", preWeight);
                                long count = (long) (r1 * preWeight / duration
                                        + r2);
                                log.info("count = {}", count);
                                return count >= rule.getAllowedNumberOfRequests();
                            }).thenApply(r -> {
                        if (r) {
                            return bucket.tryConsume(1).join();
                        }
                        return false;
                    });
                }).reduce(Boolean.TRUE, (o1, o2) -> o1 && o2.join());
    }

    private AsyncBucketProxy resolveBucket(Long key, RateLimitRule rule) {
        Supplier<CompletableFuture<BucketConfiguration>> configSupplier = getConfigSupplier(rule);
        return proxyManager.asAsync().builder().build(String.valueOf(key), configSupplier);
    }

    private Supplier<CompletableFuture<BucketConfiguration>> getConfigSupplier(RateLimitRule rule) {
        Integer requestLimit = Objects.requireNonNull(rule).getAllowedNumberOfRequests();
        Refill refill = Refill.intervally(requestLimit, Duration.ofSeconds(getDuration(rule)));
        Bandwidth limit = Bandwidth.classic(requestLimit, refill);
        return () -> (CompletableFuture.completedFuture(BucketConfiguration.builder()
                .addLimit(limit)
                .build()));
    }

    private RateLimitRule getAccount(RequestDescriptor descriptor) {
        Stream<RateLimitRule> limitRules;
        if (descriptor.getAccountId().isPresent()) {
            limitRules = rateLimitRules.stream().filter(r -> r.getAccountId().equals(descriptor.getAccountId()));
        } else {
            limitRules = rateLimitRules.stream().filter(r -> r.getClientIp().equals(descriptor.getClientIp()));
        }
        if (descriptor.getRequestType().isPresent()) {
            return limitRules.filter(r -> r.getClientIp().equals(descriptor.getRequestType())).findFirst().orElse(defaultRule);
        }
        return limitRules.findFirst().orElse(defaultRule);
    }
}
