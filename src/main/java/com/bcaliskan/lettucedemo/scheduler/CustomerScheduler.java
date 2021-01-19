package com.bcaliskan.lettucedemo.scheduler;

import com.bcaliskan.lettucedemo.configuration.RedisProperties;
import com.bcaliskan.lettucedemo.model.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerScheduler {

    private final Random random = new Random();
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${lettuce-demo.redis.fixedDelayMillis}")
    public void insertCustomer() {
        final long id = random.nextLong();
        final String name = generateString();
        final String externalId = generateString();
        final String key = redisProperties.getKeyPrefix().concat(String.valueOf(id)).concat("_").concat(name);
        Customer customer = Customer.builder()
                .id(id)
                .name(generateString())
                .externalId(generateString())
                .build();
        try {
            log.info("Inserting id={}, name={}, externalId={}", id, name, externalId);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(customer));
            redisTemplate.expire(key, redisProperties.getKeyExpireMinutes(), TimeUnit.MINUTES);
        } catch (JsonProcessingException exception) {
            log.error("", exception);
        }
        Customer retrievedCustomer = getCustomer(id, name);
        if (Objects.nonNull(retrievedCustomer))
            log.info("Retrieved customer id={}, name={}, externalId={}", retrievedCustomer.getId(),
                    retrievedCustomer.getName(), retrievedCustomer.getExternalId());
        else
            log.error("An error occured while getting customer id={} from Redis", id);
    }

    private Customer getCustomer(long id, String name) {
        try {
            final String key = redisProperties.getKeyPrefix().concat(String.valueOf(id)).concat("_").concat(name);
            final String value = (String) redisTemplate.opsForValue().get(key);
            if (Objects.isNull(value)){
                return null;
            }
            return objectMapper.readValue(value, Customer.class);
        } catch (IOException exception) {
            log.error("", exception);
            return null;
        }
    }

    private String generateString() {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ " +
                "0123456789 " +
                "abcdefghijklmnopqrstuvxyz";
        final StringBuilder stringBuilder = new StringBuilder(5);
        for (int i = 0; i < 8; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            stringBuilder.append(AlphaNumericString.charAt(index));
        }
        return stringBuilder.toString();
    }

}