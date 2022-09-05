package pizzashop.models;

import java.util.ArrayList;
import java.util.List;

public class HydratedOrder {
    public String orderId;
    public List<HydratedOrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(HydratedOrderItem item) {
        orderItems.add(item);
        orderId = item.orderId;
    }
}
