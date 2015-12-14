package io.spring.cloud.samples.brewery.acceptance.common.sleuth

trait SleuthHashing {

	long hash(String string) {
		long h = 1125899906842597L
		if (string==null) {
			return h
		}
		int len = string.length()

		for (int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i)
		}
		return h
	}

	String convertToTraceIdZipkinRequest(String traceId) {
		long hashedTraceId = hash(traceId)
		return Long.toHexString(hashedTraceId)
	}
}
