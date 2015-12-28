package io.spring.cloud.samples.brewery.acceptance.common.tech

/**
 * Properties set:
 * LOCAL_URL - for docker-machine kind of setup
 */
class ServiceUrlFetcher {

	private static final String LOCAL_MODE_URL_PROP = 'LOCAL_URL'

	private static final String LOCALHOST = "http://localhost"

	String presentingServiceUrl() {
		return "${getRootUrlForRibbon()}:9991"
	}

	private String getRootUrlForRibbon() {
		if (hasProp(LOCAL_MODE_URL_PROP)) {
			return getProp(LOCAL_MODE_URL_PROP)
		}
		return LOCALHOST
	}

	private boolean hasProp(String propName) {
		return System.getenv().containsKey(propName) ?:
				System.getProperties().containsKey(propName)
	}

	private String getProp(String propName) {
		return System.getenv(propName) ?:
				System.getProperty(propName)
	}
}
