package io.spring.cloud.samples.brewery.common.model;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<IngredientType> items = new ArrayList<>();

    public Order() {
    }

    public List<IngredientType> getItems() {
        return this.items;
    }

    public void setItems(List<IngredientType> items) {
        this.items = items;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Order)) return false;
        final Order other = (Order) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$items = this.getItems();
        final Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Order;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    public String toString() {
        return "Order(items=" + this.getItems() + ")";
    }
}
