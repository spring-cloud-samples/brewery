package io.spring.cloud.samples.brewery.common.events;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.MessageChannel;

@Configuration
public class RabbitMqConfiguration {

	private static final String CHANNEL_NAME = "events";

	@Bean
	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		Queue queue = new Queue(CHANNEL_NAME);
		admin.declareQueue(queue);
		return admin;
	}

	@Bean
	public IntegrationFlow amqpOutbound(AmqpTemplate amqpTemplate) {
		return IntegrationFlows.from(amqpOutboundChannel())
				.handle(Amqp.outboundAdapter(amqpTemplate)
						.routingKey(CHANNEL_NAME)) // default exchange - route to queue 'events'
				.get();
	}

	@Bean
	public MessageChannel amqpOutboundChannel() {
		return new DirectChannel();
	}
}
