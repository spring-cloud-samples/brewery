package io.spring.cloud.samples.brewery.reporting;

class BeerEvents {
	private String ingredientsOrderedTime;
	private String brewingStartedTime;
	private String beerMaturedTime;
	private String beerBottledTime;

	public BeerEvents() {
	}

	public String getIngredientsOrderedTime() {
		return this.ingredientsOrderedTime;
	}

	public String getBrewingStartedTime() {
		return this.brewingStartedTime;
	}

	public String getBeerMaturedTime() {
		return this.beerMaturedTime;
	}

	public String getBeerBottledTime() {
		return this.beerBottledTime;
	}

	public void setIngredientsOrderedTime(String ingredientsOrderedTime) {
		this.ingredientsOrderedTime = ingredientsOrderedTime;
	}

	public void setBrewingStartedTime(String brewingStartedTime) {
		this.brewingStartedTime = brewingStartedTime;
	}

	public void setBeerMaturedTime(String beerMaturedTime) {
		this.beerMaturedTime = beerMaturedTime;
	}

	public void setBeerBottledTime(String beerBottledTime) {
		this.beerBottledTime = beerBottledTime;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof BeerEvents)) return false;
		final BeerEvents other = (BeerEvents) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$ingredientsOrderedTime = this.getIngredientsOrderedTime();
		final Object other$ingredientsOrderedTime = other.getIngredientsOrderedTime();
		if (this$ingredientsOrderedTime == null ? other$ingredientsOrderedTime != null : !this$ingredientsOrderedTime
				.equals(other$ingredientsOrderedTime)) return false;
		final Object this$brewingStartedTime = this.getBrewingStartedTime();
		final Object other$brewingStartedTime = other.getBrewingStartedTime();
		if (this$brewingStartedTime == null ? other$brewingStartedTime != null : !this$brewingStartedTime
				.equals(other$brewingStartedTime)) return false;
		final Object this$beerMaturedTime = this.getBeerMaturedTime();
		final Object other$beerMaturedTime = other.getBeerMaturedTime();
		if (this$beerMaturedTime == null ? other$beerMaturedTime != null : !this$beerMaturedTime
				.equals(other$beerMaturedTime)) return false;
		final Object this$beerBottledTime = this.getBeerBottledTime();
		final Object other$beerBottledTime = other.getBeerBottledTime();
		if (this$beerBottledTime == null ? other$beerBottledTime != null : !this$beerBottledTime
				.equals(other$beerBottledTime)) return false;
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof BeerEvents;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $ingredientsOrderedTime = this.getIngredientsOrderedTime();
		result = result * PRIME + ($ingredientsOrderedTime == null ? 43 : $ingredientsOrderedTime.hashCode());
		final Object $brewingStartedTime = this.getBrewingStartedTime();
		result = result * PRIME + ($brewingStartedTime == null ? 43 : $brewingStartedTime.hashCode());
		final Object $beerMaturedTime = this.getBeerMaturedTime();
		result = result * PRIME + ($beerMaturedTime == null ? 43 : $beerMaturedTime.hashCode());
		final Object $beerBottledTime = this.getBeerBottledTime();
		result = result * PRIME + ($beerBottledTime == null ? 43 : $beerBottledTime.hashCode());
		return result;
	}

	public String toString() {
		return "BeerEvents(ingredientsOrderedTime=" + this.getIngredientsOrderedTime() + ", brewingStartedTime=" + this
				.getBrewingStartedTime() + ", beerMaturedTime=" + this
				.getBeerMaturedTime() + ", beerBottledTime=" + this.getBeerBottledTime() + ")";
	}
}
