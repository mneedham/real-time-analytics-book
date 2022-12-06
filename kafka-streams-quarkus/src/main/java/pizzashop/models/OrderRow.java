package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OrderRow {

    private final String dateTime;
    private final double price;
    private final long userId;
    private final long productsOrdered;
    private final long totalQuantity;

    public OrderRow(String dateTime, double price, long userId, long productsOrdered , long totalQuantity) {

        this.dateTime = dateTime;
        this.price = price;
        this.userId = userId;
        this.productsOrdered = productsOrdered;
        this.totalQuantity = totalQuantity;
    }

    public String getDateTime() {
        return dateTime;
    }

    public double getPrice() {
        return price;
    }

    public long getUserId() {
        return userId;
    }

    public long getProductsOrdered() {
        return productsOrdered;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }
}
