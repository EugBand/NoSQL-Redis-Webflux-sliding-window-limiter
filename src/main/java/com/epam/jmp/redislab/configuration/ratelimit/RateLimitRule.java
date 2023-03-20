package com.epam.jmp.redislab.configuration.ratelimit;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RateLimitRule {

    private final Optional<String> accountId;

    private final Optional<String> clientIp;

    private final Optional<String> requestType;

    private final Integer allowedNumberOfRequests;

    private final RateLimitTimeInterval timeInterval;

    @JsonCreator
    public RateLimitRule(
            @JsonProperty("accountId")
            Optional<String> accountId,
            @JsonProperty("clientIp")
            Optional<String> clientIp,
            @JsonProperty("requestType")
            Optional<String> requestType,
            @JsonProperty("allowedNumberOfRequests")
            Integer allowedNumberOfRequests,
            @JsonProperty("timeInterval")
            RateLimitTimeInterval timeInterval) {
        this.accountId = accountId;
        this.clientIp = clientIp;
        this.requestType = requestType;
        this.allowedNumberOfRequests = allowedNumberOfRequests;
        this.timeInterval = timeInterval;
    }
}
