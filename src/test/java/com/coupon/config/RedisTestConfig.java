package com.coupon.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import jakarta.annotation.PreDestroy;

@TestConfiguration
public class RedisTestConfig {

    private static final String REDIS_IMAGE = "redis:7.0.0";
    private static final int REDIS_PORT = 6379;
    
    private GenericContainer<?> redisContainer;
    
    @Bean
    @SuppressWarnings("resource")
    public GenericContainer<?> redisContainer() {
        if (redisContainer == null) {
            redisContainer = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                    .withExposedPorts(REDIS_PORT)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
            redisContainer.start();
            
            // Set system properties for test configuration
            System.setProperty("REDIS_HOST", redisContainer.getHost());
            System.setProperty("REDIS_PORT", String.valueOf(redisContainer.getMappedPort(REDIS_PORT)));
            
            System.out.println("Test Redis container started at: " + 
                    redisContainer.getHost() + ":" + redisContainer.getMappedPort(REDIS_PORT));
        }
        return redisContainer;
    }
    
    @PreDestroy
    public void stopRedis() {
        if (redisContainer != null) {
            System.out.println("Stopping Test Redis container...");
            redisContainer.stop();
        }
    }
}
