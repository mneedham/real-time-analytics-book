package pizzashop.models;

import java.util.List;

public class Order {
    public Order() {

    }

    public String id;
    public String userId;
    public String createdAt;

    public double price;

    public double deliveryLat;
    public double deliveryLon;

    public List<OrderItem> items;

//    public int productId;
//    public int quantity;
//    public double total;
}
