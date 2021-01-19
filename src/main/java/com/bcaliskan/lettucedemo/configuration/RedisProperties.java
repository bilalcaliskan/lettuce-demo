package com.bcaliskan.lettucedemo.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lettuce-demo.redis")
public class RedisProperties {

    private String master;
    private List<String> nodes;
    private String keyPrefix;
    private int poolMinIdle;
    private int poolMaxIdle;
    private int poolMaxTotal;
    private long poolMaxWaitMillis;
    private int keyExpireMinutes;
    private long fixedDelayMillis;
    private long timeoutMillis;

}