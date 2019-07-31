package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("stubbed.ingredients")
class StubbedIngredientsProperties {

	/**
     * Set to 1000 to correlate with what comes back from config-server.
	 * The default value of threshold is 2000. The one from config-server is 1000.
	 *
	 * If value from config server is not passed then no beer will be brewed.
     */
    private Integer returnedIngredientsQuantity = 1000;

	public StubbedIngredientsProperties() {
	}

	public Integer getReturnedIngredientsQuantity() {
		return this.returnedIngredientsQuantity;
	}

	public void setReturnedIngredientsQuantity(Integer returnedIngredientsQuantity) {
		this.returnedIngredientsQuantity = returnedIngredientsQuantity;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof StubbedIngredientsProperties)) return false;
		final StubbedIngredientsProperties other = (StubbedIngredientsProperties) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$returnedIngredientsQuantity = this.getReturnedIngredientsQuantity();
		final Object other$returnedIngredientsQuantity = other.getReturnedIngredientsQuantity();
		if (this$returnedIngredientsQuantity == null ? other$returnedIngredientsQuantity != null : !this$returnedIngredientsQuantity
				.equals(other$returnedIngredientsQuantity)) return false;
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof StubbedIngredientsProperties;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $returnedIngredientsQuantity = this.getReturnedIngredientsQuantity();
		result = result * PRIME + ($returnedIngredientsQuantity == null ? 43 : $returnedIngredientsQuantity.hashCode());
		return result;
	}

	public String toString() {
		return "StubbedIngredientsProperties(returnedIngredientsQuantity=" + this
				.getReturnedIngredientsQuantity() + ")";
	}
}
