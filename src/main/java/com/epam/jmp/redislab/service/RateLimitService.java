package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitTimeInterval;

import java.util.Set;

public interface RateLimitService {

    default boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        return false;
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
