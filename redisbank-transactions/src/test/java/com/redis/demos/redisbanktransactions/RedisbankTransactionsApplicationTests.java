package com.redis.demos.redisbanktransactions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class RedisbankTransactionsApplicationTests {

	static final String REDIS_IMAGE = "redis/redis-stack-server:latest";
	static final int REDIS_PORT = 6379;

	@Container
	static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
			.withExposedPorts(REDIS_PORT);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.redis.host", redisContainer::getContainerIpAddress);
		registry.add("spring.redis.port", redisContainer::getFirstMappedPort);
		registry.add("spring.redis.host-tr", redisContainer::getContainerIpAddress);
		registry.add("spring.redis.port-tr", redisContainer::getFirstMappedPort);
	}

	@Test
	void contextLoads() {
	}

}
