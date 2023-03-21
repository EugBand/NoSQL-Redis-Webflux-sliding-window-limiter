package com.epam.jmp.redislab.configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.command.CommandExecutor;
import org.redisson.command.CommandSyncService;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;

@Configuration
public class RedissonConfiguration {

    @Bean
    public Config config() {
        Config config = new Config();
        config.useClusterServers().addNodeAddress("redis://localhost:30000", "redis://localhost:30001", "redis://localhost:30002");
        return config;
    }

//    @Bean(name = "springCM")
//    public CacheManager cacheManager(Config config) {
//        CacheManager manager = Caching.getCachingProvider().getCacheManager();
//        manager.createCache("cache", org.redisson.jcache.configuration.RedissonConfiguration.fromConfig(config));
//        manager.createCache("RateLimitRules", org.redisson.jcache.configuration.RedissonConfiguration.fromConfig(config));
//        return manager;
//    }

    @Bean
    ProxyManager<String> proxyManager(Config config) {
        RedissonObjectBuilder objectBuilder = new RedissonObjectBuilder(createReactiveClient(config));
        ConnectionManager connectionManager = ConfigSupport.createConnectionManager(config);
        CommandExecutor commandExecutor = new CommandSyncService(connectionManager, objectBuilder);
        return RedissonBasedProxyManager.builderFor(commandExecutor)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofHours(1)))
                .build();
    }

    @Bean
    RedissonClient createClient(Config config) {
        return Redisson.create(config);
    }

    @Bean
    RedissonReactiveClient createReactiveClient(Config config) {
        return Redisson.createReactive(config);
    }

}
