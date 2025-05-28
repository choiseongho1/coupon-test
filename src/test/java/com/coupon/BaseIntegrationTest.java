package com.coupon;

import com.coupon.config.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest extends TestContainersConfig {
    
    // Redis TestContainer 설정
    private static final String REDIS_IMAGE = "redis:7.0";
    private static final int REDIS_PORT = 6379;
    
    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        // Redis TestContainer 설정 동적 적용
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT).toString());
        
        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.dialect", 
            () -> "org.hibernate.dialect.MySQL8Dialect");
    }
    
    @BeforeEach
    void setUp() {
        // 각 테스트 전에 실행할 공통 설정
    }
}
