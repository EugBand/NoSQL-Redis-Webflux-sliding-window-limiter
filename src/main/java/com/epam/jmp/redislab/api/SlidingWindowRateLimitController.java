package com.epam.jmp.redislab.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epam.jmp.redislab.service.RateLimitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ratelimit/fixedwindow")
public class SlidingWindowRateLimitController {

    @Autowired
    private final RateLimitService rateLimitService;

    @PostMapping
    public ResponseEntity<Void> shouldRateLimit(
            @RequestBody
            RateLimitRequest rateLimitRequest) {
        if (rateLimitService.shouldLimit(rateLimitRequest.getDescriptors())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok().build();
    }
}
