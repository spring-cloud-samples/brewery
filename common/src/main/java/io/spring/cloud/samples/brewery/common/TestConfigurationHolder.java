package io.spring.cloud.samples.brewery.common;

public class TestConfigurationHolder {
	public static final String TEST_COMMUNICATION_TYPE_HEADER_NAME = "TEST-COMMUNICATION-TYPE";
	public static final ThreadLocal<TestConfigurationHolder> TEST_CONFIG = new ThreadLocal<>();

	private TestCommunicationType testCommunicationType;

	@java.beans.ConstructorProperties({"testCommunicationType"})
	TestConfigurationHolder(TestCommunicationType testCommunicationType) {
		this.testCommunicationType = testCommunicationType;
	}

	public static TestConfigurationHolderBuilder builder() {
		return new TestConfigurationHolderBuilder();
	}

	public TestCommunicationType getTestCommunicationType() {
		return this.testCommunicationType;
	}

	public void setTestCommunicationType(TestCommunicationType testCommunicationType) {
		this.testCommunicationType = testCommunicationType;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof TestConfigurationHolder)) return false;
		final TestConfigurationHolder other = (TestConfigurationHolder) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$testCommunicationType = this.getTestCommunicationType();
		final Object other$testCommunicationType = other.getTestCommunicationType();
		if (this$testCommunicationType == null ? other$testCommunicationType != null : !this$testCommunicationType
				.equals(other$testCommunicationType)) return false;
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TestConfigurationHolder;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $testCommunicationType = this.getTestCommunicationType();
		result = result * PRIME + ($testCommunicationType == null ? 43 : $testCommunicationType.hashCode());
		return result;
	}

	public String toString() {
		return "TestConfigurationHolder(testCommunicationType=" + this.getTestCommunicationType() + ")";
	}

	public enum TestCommunicationType {
		FEIGN, REST_TEMPLATE
	}

	public static class TestConfigurationHolderBuilder {
		private TestCommunicationType testCommunicationType;

		TestConfigurationHolderBuilder() {
		}

		public TestConfigurationHolder.TestConfigurationHolderBuilder testCommunicationType(TestCommunicationType testCommunicationType) {
			this.testCommunicationType = testCommunicationType;
			return this;
		}

		public TestConfigurationHolder build() {
			return new TestConfigurationHolder(testCommunicationType);
		}

		public String toString() {
			return "TestConfigurationHolder.TestConfigurationHolderBuilder(testCommunicationType=" + this.testCommunicationType + ")";
		}
	}
}
