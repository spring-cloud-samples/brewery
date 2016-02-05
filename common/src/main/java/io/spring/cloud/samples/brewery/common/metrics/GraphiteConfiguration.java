package io.spring.cloud.samples.brewery.common.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteUDP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Configuration
public class GraphiteConfiguration {

	@Value("${HEALTH_HOST:localhost}") String graphiteHost;
	@Value("${graphite.port:3003}") int graphitePort;

	@Bean public GraphiteReporter graphiteReporter(MetricRegistry metricRegistry) {
		GraphiteUDP graphite = new GraphiteUDP(new InetSocketAddress(graphiteHost, graphitePort));
		GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
				.prefixedWith("boot").build(graphite);
		reporter.start(500, TimeUnit.MILLISECONDS);
		return reporter;
	}
}
