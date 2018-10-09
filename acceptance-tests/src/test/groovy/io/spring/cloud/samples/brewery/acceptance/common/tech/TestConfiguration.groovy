package io.spring.cloud.samples.brewery.acceptance.common.tech
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration(exclude = GroovyTemplateAutoConfiguration)
class TestConfiguration {

}
