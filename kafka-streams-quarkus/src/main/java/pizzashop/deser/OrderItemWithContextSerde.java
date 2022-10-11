package pizzashop.deser;

import org.apache.kafka.common.serialization.Serdes;
import pizzashop.models.OrderItemWithContext;

public class OrderItemWithContextSerde extends Serdes.WrapperSerde<OrderItemWithContext> {
    public OrderItemWithContextSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(OrderItemWithContext.class));
    }
}
