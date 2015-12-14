package io.spring.cloud.samples.brewery.common;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.cloud.sleuth.zipkin.EndpointLocator;
import org.springframework.cloud.sleuth.zipkin.ServerPropertiesEndpointLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestConfiguration {

    @Bean
    public FilterRegistrationBean correlationIdFilter() {
        return new FilterRegistrationBean(new TestConfigurationSettingFilter());
    }

    @Bean
    @Primary
    EndpointLocator endpointLocator(ServerProperties serverProperties) {
        return new ServerPropertiesEndpointLocator(serverProperties);
    }

}
