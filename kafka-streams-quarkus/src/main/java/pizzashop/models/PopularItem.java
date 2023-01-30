package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PopularItem {


    private final String product;
    private final String image;
    private final long orders;
    private final double quantity;

    public PopularItem(String product, String image, long orders, double quantity) {

        this.product = product;
        this.image = image;
        this.orders = orders;
        this.quantity = quantity;
    }

    public String getProduct() {
        return product;
    }

    public String getImage() {
        return image;
    }

    public long getOrders() {
        return orders;
    }

    public double getQuantity() {
        return quantity;
    }
}
