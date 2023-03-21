package com.epam.jmp.redislab.model;

import io.github.bucket4j.distributed.AsyncBucketProxy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterState {

    Boolean isNotLimited;
    AsyncBucketProxy curBucket;

}
