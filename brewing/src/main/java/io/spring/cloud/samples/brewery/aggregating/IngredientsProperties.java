package io.spring.cloud.samples.brewery.aggregating;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ingredients")
class IngredientsProperties {

    private Integer threshold = 2000;

    public IngredientsProperties() {
    }

    public Integer getThreshold() {
        return this.threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof IngredientsProperties)) return false;
        final IngredientsProperties other = (IngredientsProperties) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$threshold = this.getThreshold();
        final Object other$threshold = other.getThreshold();
        if (this$threshold == null ? other$threshold != null : !this$threshold.equals(other$threshold)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof IngredientsProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $threshold = this.getThreshold();
        result = result * PRIME + ($threshold == null ? 43 : $threshold.hashCode());
        return result;
    }

    public String toString() {
        return "IngredientsProperties(threshold=" + this.getThreshold() + ")";
    }
}
