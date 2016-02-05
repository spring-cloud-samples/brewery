package io.spring.cloud.samples.brewery.common.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Configuration
public class GraphiteConfiguration {

	@Value("${HEALTH_HOST:localhost}") String graphiteHost;
	@Value("${graphite.port:3003}") int graphitePort;
	@Value("${spring.application.name}") String appName;

	@Bean public GraphiteReporter graphiteReporter(MetricRegistry metricRegistry) {
		GraphiteSender graphite = new ExceptionIgnoringGraphite(new Graphite(new InetSocketAddress(graphiteHost, graphitePort)));
		GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
				.prefixedWith(appName).build(graphite);
		reporter.start(5000, TimeUnit.MILLISECONDS);
		return reporter;
	}


	@Slf4j
	public static class ExceptionIgnoringGraphite implements GraphiteSender {

		private final GraphiteSender graphiteSender;

		ExceptionIgnoringGraphite(GraphiteSender graphiteSender) {
			this.graphiteSender = graphiteSender;
		}

		@Override
		public void connect() throws IllegalStateException, IOException {
			doAndLogException(graphiteSender::connect);
		}

		@Override
		public void send(String name, String value, long timestamp) throws IOException {
			doAndLogException(() -> graphiteSender.send(name, value, timestamp));
		}

		@Override
		public void flush() throws IOException {
			doAndLogException(graphiteSender::flush);
		}

		@Override
		public boolean isConnected() {
			return graphiteSender.isConnected();
		}

		@Override
		public int getFailures() {
			return graphiteSender.getFailures();
		}

		@Override
		public void close() throws IOException {
			doAndLogException(graphiteSender::close);
		}

		void doAndLogException(Ignorable ignorable) {
			try {
				ignorable.doAndLogException();
			} catch (Exception e) {
				log.trace("Exception occurred while trying to connect to Graphite [{}]", e.getMessage());
			}
		}
	}

	interface Ignorable {
		void doAndLogException() throws Exception;
	}
}
