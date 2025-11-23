package com.ecommerce.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Central Testcontainers bootstrap used by integration tests.
 * Starts Postgres and Redis and sets system properties used by Spring Boot test
 * context.
 *
 * This class uses static initialization so that containers start once per JVM.
 * Real-world teams keep this centralized to avoid duplicate container startups.
 */
public abstract class TestContainersConfig {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        // start containers
        POSTGRES.start();
        REDIS.start();

        // set Spring datasource properties for tests
        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());

        // redis configuration used by redis auto-config (RedisConfig reads host/port
        // from properties)
        System.setProperty("spring.redis.host", REDIS.getHost());
        System.setProperty("spring.redis.port", String.valueOf(REDIS.getMappedPort(6379)));
    }

    // Prevent instantiation
    private TestContainersConfig() {
    }
}
