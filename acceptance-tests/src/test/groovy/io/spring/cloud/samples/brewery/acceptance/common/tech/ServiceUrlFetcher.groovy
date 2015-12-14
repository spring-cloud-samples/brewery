package io.spring.cloud.samples.brewery.acceptance.common.tech

import org.springframework.core.env.Environment

/**
 * Properties set:
 * LOCAL_URL - for docker-machine kind of setup
 * LOCAL - will shoot at http://localhost
 *
 * LOCAL will be also working if 'local' spring profile is set
 *
 * If neither of those are set the tests will try to use Ribbon to find
 * the presenting service
 */
class ServiceUrlFetcher {

	public static final String LOCAL_MODE_PROP = 'LOCAL'
	public static final String LOCAL_MODE_URL_PROP = 'LOCAL_URL'

	private static final String LOCALHOST = "http://localhost"
	private static final String LOCAL_SPRING_PROFILE = 'local'

	private final Environment environment

	ServiceUrlFetcher(Environment environment) {
		this.environment = environment
	}

	String presentingServiceUrl() {
		String rootUrl = getRootUrlForRibbon()
		if (isLocalMode()) {
			return "$rootUrl:9991"
		}
		return "${stripTrailingSlash(rootUrl)}/presenting"
	}

	private boolean isLocalMode() {
		return localSpringModeIsActive() ||
				hasProp(LOCAL_MODE_PROP) || hasProp(LOCAL_MODE_URL_PROP)
	}

	private boolean localSpringModeIsActive() {
		return environment.getActiveProfiles().contains(LOCAL_SPRING_PROFILE)
	}

	private String stripTrailingSlash(String url) {
		if (url.endsWith('/')) {
			return url.substring(0, url.length() - 1)
		}
		return url
	}

	private String getRootUrlForRibbon() {
		if (hasProp(LOCAL_MODE_PROP)) {
			return LOCALHOST
		} else if (hasProp(LOCAL_MODE_URL_PROP)) {
			return getProp(LOCAL_MODE_URL_PROP)
		} else if (localSpringModeIsActive()) {
			return LOCALHOST
		}
		return "http://"
	}

	private boolean hasProp(String propName) {
		return environment.hasProperty(propName)
	}

	private String getProp(String propName) {
		return environment.getProperty(propName)
	}
}
