package com.bcaliskan.lettucedemo.configuration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    private static final String NODE_SPLITTER = ":";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory lettuceConnectionFactory) {
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public RedisConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties, LettucePoolingClientConfiguration lettucePoolingClientConfiguration) {
        final RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration().master(redisProperties.getMaster());
        addSentinels(redisProperties, redisSentinelConfiguration);
        return new LettuceConnectionFactory(redisSentinelConfiguration, lettucePoolingClientConfiguration);
    }

    @Bean
    public LettucePoolingClientConfiguration lettucePoolingClientConfiguration(ClientOptions clientOptions,
                                                                               ClientResources clientResources,
                                                                               RedisProperties redisProperties) {
        return LettucePoolingClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA)
                .poolConfig(genericObjectPoolConfig(redisProperties))
                .clientOptions(clientOptions)
                .clientResources(clientResources)
                .build();
    }

    @Bean
    public GenericObjectPoolConfig genericObjectPoolConfig(RedisProperties redisProperties) {
        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(redisProperties.getPoolMaxIdle());
        config.setMinIdle(redisProperties.getPoolMinIdle());
        config.setMaxTotal(redisProperties.getPoolMaxTotal());
        config.setBlockWhenExhausted(false);
        config.setMaxWaitMillis(redisProperties.getPoolMaxWaitMillis());
        return config;
    }

    @Bean
    public ClientOptions clientOptions(RedisProperties redisProperties) {
        return ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofMillis(redisProperties.getTimeoutMillis()))
                        .build())
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    private void addSentinels(RedisProperties redisProperties, RedisSentinelConfiguration redisSentinelConfiguration) {
        redisProperties.getNodes()
                .forEach(node -> {
                    final String[] splitted = node.split(NODE_SPLITTER);
                    final String host = splitted[0];
                    final int port = Integer.parseInt(splitted[1]);
                    redisSentinelConfiguration.addSentinel(RedisNode.newRedisNode()
                            .listeningAt(host, port)
                            .build());
                });
    }

}