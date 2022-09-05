package pizzashop.deser;

import org.apache.kafka.common.serialization.Serdes;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.models.OrderItem;

public class OrderItemSerde extends Serdes.WrapperSerde<OrderItem> {
    public OrderItemSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(OrderItem.class));
    }
}
