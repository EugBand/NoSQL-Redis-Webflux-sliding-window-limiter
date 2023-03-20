package com.epam.jmp.redislab.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@Configuration
public class RateLimitConfiguration {

    private final ObjectMapper objectMapper;

    public RateLimitConfiguration() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.registerModule(new Jdk8Module());
    }

    @Bean
    public Set<RateLimitRule> rateLimitRules() throws IOException {
        ClassPathResource rateLimitConfigurationResource = new ClassPathResource("ratelimitRules.yaml");
        InputStream inputStream = rateLimitConfigurationResource.getInputStream();
        return new HashSet<>(Arrays.asList(objectMapper.readValue(inputStream, RateLimitRule[].class)));
    }

    @Bean
    public RateLimitRule getDefaultRule() throws IOException {
        return
                rateLimitRules().stream()
                        .filter(r -> "".equals(r.getAccountId().orElse(null)))
                        .filter(r -> !r.getRequestType().isPresent())
                        .findFirst().orElse(null);
    }

}
