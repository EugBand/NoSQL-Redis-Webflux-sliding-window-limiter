package com.epam.jmp.redislab.configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;


@Configuration
public class RedissonConfiguration {

    @Bean
    public Config config() {
        Config config = new Config();
        config.useClusterServers().addNodeAddress("redis://localhost:30000", "redis://localhost:30001", "redis://localhost:30002");
        return config;
    }

    @Bean(name = "springCM")
    public CacheManager cacheManager(Config config) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        manager.createCache("cache", org.redisson.jcache.configuration.RedissonConfiguration.fromConfig(config));
        manager.createCache("RateLimitRules", org.redisson.jcache.configuration.RedissonConfiguration.fromConfig(config));
        return manager;
    }

    @Bean
    ProxyManager<String> proxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache("cache"));
    }

    @Bean
    RedissonClient createClient(Config config){
        return Redisson.create(config);
    }

}
