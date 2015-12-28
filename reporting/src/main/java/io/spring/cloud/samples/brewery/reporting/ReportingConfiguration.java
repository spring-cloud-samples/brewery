package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.RabbitMqConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@Import(RabbitMqConfiguration.class)
@Slf4j
public class ReportingConfiguration {

	@Bean
	public MessageChannel amqpInputChannel() {
		return new DirectChannel();
	}

	@Bean
	public AmqpInboundGateway inbound(SimpleMessageListenerContainer listenerContainer,
									  @Qualifier("amqpInputChannel") MessageChannel channel) {
		AmqpInboundGateway gateway = new AmqpInboundGateway(listenerContainer);
		gateway.setRequestChannel(channel);
		return gateway;
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
		SimpleMessageListenerContainer container =
				new SimpleMessageListenerContainer(connectionFactory);
		container.setQueueNames("events");
		container.setConcurrentConsumers(2);
		return container;
	}

	@Bean
	ReportingRepository reportingDatabase() {
		return new ReportingRepository();
	}

}
