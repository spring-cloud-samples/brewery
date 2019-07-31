package io.spring.cloud.samples.brewery.common.model;

public class Wort {
    private int wort;

    public Wort(int wort) {
        this.wort = wort;
    }

    public int getWort() {
        return this.wort;
    }

    public void setWort(int wort) {
        this.wort = wort;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Wort)) return false;
        final Wort other = (Wort) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getWort() != other.getWort()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Wort;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getWort();
        return result;
    }

    public String toString() {
        return "Wort(wort=" + this.getWort() + ")";
    }
}
