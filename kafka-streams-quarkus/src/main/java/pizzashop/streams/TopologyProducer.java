package pizzashop.streams;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.deser.OrderItemWithContextSerde;
import pizzashop.models.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@ApplicationScoped
public class TopologyProducer {
    @Produces
    public Topology buildTopology() {
        String ordersTopic = System.getenv().getOrDefault("ORDERS_TOPIC",  "orders-multi9");
        String productsTopic = System.getenv().getOrDefault("PRODUCTS_TOPIC",  "products-multi9");
        String enrichedOrdersTopic = System.getenv().getOrDefault("ENRICHED_ORDERS_TOPIC",  "enriched-orders-multi10");

        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Order.class));
        OrderItemWithContextSerde orderItemWithContextSerde = new OrderItemWithContextSerde();
        final Serde<Product> productSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Product.class));
        final Serde<HydratedOrderItem> hydratedOrderItemsSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrderItem.class));
        final Serde<HydratedOrder> hydratedOrdersSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrder.class));
        final Serde<CompleteOrder> completeOrderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(CompleteOrder.class));

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Order> orders = builder.stream(ordersTopic, Consumed.with(Serdes.String(), orderSerde));
        KTable<String, Product> products = builder.table(productsTopic, Consumed.with(Serdes.String(), productSerde));

        KStream<String, OrderItemWithContext> orderItems = orders.flatMap((key, value) -> {
            List<KeyValue<String, OrderItemWithContext>> result = new ArrayList<>();
            for (OrderItem item : value.items) {
                OrderItemWithContext orderItemWithContext = new OrderItemWithContext();
                orderItemWithContext.orderId = value.id;
                orderItemWithContext.createdAt = value.createdAt;
                orderItemWithContext.orderItem = item;
                result.add(new KeyValue<>(String.valueOf(item.productId), orderItemWithContext));
            }
            return result;
        });

        orderItems.join(products, (orderItem, product) -> {
                    HydratedOrderItem hydratedOrderItem = new HydratedOrderItem();
                    hydratedOrderItem.createdAt = orderItem.createdAt;
                    hydratedOrderItem.orderId = orderItem.orderId;
                    hydratedOrderItem.orderItem = orderItem.orderItem;
                    hydratedOrderItem.product = product;
                    return hydratedOrderItem;
                }, Joined.with(Serdes.String(), orderItemWithContextSerde, productSerde))
                .to(enrichedOrdersTopic, Produced.with(Serdes.String(), hydratedOrderItemsSerde));

        final Properties props = new Properties();

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, OrderItemWithContextSerde.class.getName());
        return builder.build(props);
    }
}