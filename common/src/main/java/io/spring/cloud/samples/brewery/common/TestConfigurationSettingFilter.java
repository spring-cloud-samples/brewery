package io.spring.cloud.samples.brewery.common;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class TestConfigurationSettingFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
		String testCommunicationTypeAsString = getHeader(httpServletRequest, httpServletResponse, TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME);
		TestConfigurationHolder.TestCommunicationType type;
		if (StringUtils.hasText(testCommunicationTypeAsString)) {
			type = TestConfigurationHolder.TestCommunicationType.valueOf(testCommunicationTypeAsString);
		} else {
			type = TestConfigurationHolder.TestCommunicationType.REST_TEMPLATE;
		}
		TestConfigurationHolder.TEST_CONFIG.set(TestConfigurationHolder.builder().testCommunicationType(type).build());
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}

	private String getHeader(HttpServletRequest request, HttpServletResponse response,
							 String name) {
		String value = request.getHeader(name);
		return StringUtils.hasText(value) ? value : response.getHeader(name);
	}
}
