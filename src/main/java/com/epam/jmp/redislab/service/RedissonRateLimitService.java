package com.epam.jmp.redislab.service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        boolean isLimited = true;
        for (RequestDescriptor descriptor : requestDescriptors) {
            RateLimitRule rule = getAccount(descriptor);
            int duration = getDuration(rule);
            long curTime = System.currentTimeMillis() / 1000;
            long curWindowKey = curTime / duration * duration;
            Bucket bucket = resolveBucket(curWindowKey, rule);
            long preWindowKey = curWindowKey - duration;
            log.info("curWindowKey = {} ; preWindowKey = {}", curWindowKey, preWindowKey);

            Bucket preBucket = resolveBucket(preWindowKey, rule);
            log.info("preCount = {}", preBucket.getAvailableTokens());
            if (preBucket.getAvailableTokens() == rule.getAllowedNumberOfRequests() &&
                    bucket.getAvailableTokens() == rule.getAllowedNumberOfRequests()) {
                isLimited = !bucket.tryConsume(1);
                continue;
            }

            double preWeight = (curTime - curWindowKey);
            log.info("preWeight = {}", preWeight);
            long count = (long) (preBucket.getAvailableTokens() * preWeight / duration
                    + bucket.getAvailableTokens());
            log.info("count = {}", count);
            if (count >= rule.getAllowedNumberOfRequests()) {
                isLimited = !bucket.tryConsume(1);
            }
        }
        log.info("Is limited = {}", isLimited);
        return isLimited;
    }


    private Bucket resolveBucket(Long key, RateLimitRule rule) {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplier(rule);
        return proxyManager.builder().build(String.valueOf(key), configSupplier);
    }

    private Supplier<BucketConfiguration> getConfigSupplier(RateLimitRule rule) {
        Integer requestLimit = Objects.requireNonNull(rule).getAllowedNumberOfRequests();
        Refill refill = Refill.intervally(requestLimit, Duration.ofSeconds(getDuration(rule)));
        Bandwidth limit = Bandwidth.classic(requestLimit, refill);
        return () -> (BucketConfiguration.builder()
                .addLimit(limit)
                .build());
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
