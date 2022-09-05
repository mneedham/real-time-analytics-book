package pizzashop.deser;

import org.apache.kafka.common.serialization.Serdes;
import pizzashop.models.OrderItem;
import pizzashop.models.OrderItemWithOrderId;

public class OrderItemWithOrderIdSerde extends Serdes.WrapperSerde<OrderItemWithOrderId> {
    public OrderItemWithOrderIdSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(OrderItemWithOrderId.class));
    }
}
