package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SummaryRow {
    private final String timestamp;
    private final long orders;
    private final double revenue;

    public SummaryRow(String timestamp, long orders, double revenue) {
        this.timestamp = timestamp;
        this.orders = orders;
        this.revenue = revenue;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public long getOrders() {
        return orders;
    }

    public double getRevenue() {
        return revenue;
    }
}
