package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PopularCategory {


    private final String category;
    private final long orders;
    private final double quantity;

    public PopularCategory(String category, long orders, double quantity) {

        this.category = category;
        this.orders = orders;
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }


    public long getOrders() {
        return orders;
    }

    public double getQuantity() {
        return quantity;
    }
}
