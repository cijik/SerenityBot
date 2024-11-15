package com.ciji.serenity;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

@SpringBootTest
class ApplicationTest {

	@Test
	void contextLoads() {
	}

	@Configuration
	static class TestConfig {

		@Bean
		@SuppressWarnings("unchecked")
		public RedisSerializer<Object> defaultRedisSerializer()
		{
			return Mockito.mock(RedisSerializer.class);
		}

		@Bean
		public RedisConnectionFactory connectionFactory()
		{
			RedisConnectionFactory factory = Mockito.mock(RedisConnectionFactory.class);
			RedisConnection connection = Mockito.mock(RedisConnection.class);
			Mockito.when(factory.getConnection()).thenReturn(connection);

			return factory;
		}
	}

}
