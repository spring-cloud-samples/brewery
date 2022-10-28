package io.spring.cloud.samples.brewery.maturing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("brew")
class BrewProperties {

    private Long timeout = 10L;

    public BrewProperties() {
    }

    public Long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof BrewProperties)) return false;
        final BrewProperties other = (BrewProperties) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$timeout = this.getTimeout();
        final Object other$timeout = other.getTimeout();
        if (this$timeout == null ? other$timeout != null : !this$timeout.equals(other$timeout)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof BrewProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $timeout = this.getTimeout();
        result = result * PRIME + ($timeout == null ? 43 : $timeout.hashCode());
        return result;
    }

    public String toString() {
        return "BrewProperties(timeout=" + this.getTimeout() + ")";
    }
}
