package com.epam.jmp.redislab.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component("concurrentmap")
@RequiredArgsConstructor
public class ConcurrentMapRateLimitService implements RateLimitService {

    private final ConcurrentMap<Long, AtomicInteger> windows = new ConcurrentHashMap<>();
    @Autowired
    Set<RateLimitRule> rateLimitRules;
    @Autowired
    RateLimitRule defaultRule;

    @Override
    public Mono<Boolean> shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        boolean isLimited = false;
        for (RequestDescriptor descriptor : requestDescriptors) {
            RateLimitRule rule = getAccount(descriptor);
            int duration = getDuration(rule);
            long curTime = System.currentTimeMillis() / 1000;
            long curWindowKey = curTime / duration * duration;
            windows.putIfAbsent(curWindowKey, new AtomicInteger(0));
            long preWindowKey = curWindowKey - duration;

            AtomicInteger preCount = windows.get(preWindowKey);
            log.info("preCount = {}", preCount);
            if (preCount == null) {
                isLimited = windows.get(curWindowKey).incrementAndGet() > rule.getAllowedNumberOfRequests();
                if (isLimited) {
                    windows.get(curWindowKey).decrementAndGet();
                }
                continue;
            }

            double preWeight = duration - (curTime - curWindowKey);
            log.info("preWeight = {}", preWeight);
            long count = (long) (preCount.get() * preWeight / duration
                    + windows.get(curWindowKey).incrementAndGet());
            log.info("count = {}", count);
            isLimited = count > rule.getAllowedNumberOfRequests();
            if (isLimited) {
                windows.get(curWindowKey).decrementAndGet();
            }
        }
        return Mono.just(isLimited);
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
