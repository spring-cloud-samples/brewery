package io.spring.cloud.samples.brewery.common.events;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;

@Configuration
public class RabbitMqConfiguration {

	private static final String CHANNEL_NAME = "events";

	@Bean
	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	Queue eventsQueue() {
		return new Queue(CHANNEL_NAME);
	}

	@Bean
	EventGateway eventGateway() {
		return new EventGateway();
	}
}
