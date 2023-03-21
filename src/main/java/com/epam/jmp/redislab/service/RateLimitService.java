package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitTimeInterval;

import java.util.Set;

import reactor.core.publisher.Mono;

public interface RateLimitService {

    default Mono<Boolean> shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        return Mono.just(false);
    }

    default int getDuration(RateLimitRule rule) {
        if (rule.getTimeInterval().equals(RateLimitTimeInterval.HOUR)) {
            return 3600;
        }
        if (rule.getTimeInterval().equals(RateLimitTimeInterval.MINUTE)) {
            return 60;
        }
        return 0;
    }
}
