package pizzashop.models;

import java.util.ArrayList;
import java.util.List;

public class CompleteOrder {
    public String id;
    public String userId;
    public String status;
    public String createdAt;
    public List<HydratedOrderItem> orderItems = new ArrayList<>();

}
