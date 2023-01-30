package pizzashop.streams;

import io.debezium.serde.DebeziumSerdes;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.deser.OrderItemWithContextSerde;
import pizzashop.deser.ProductDeserializer;
import pizzashop.models.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@ApplicationScoped
public class Topology {
    @Produces
    public org.apache.kafka.streams.Topology buildTopology() {
        String orderStatusesTopic = System.getenv().getOrDefault("ORDER_STATUSES_TOPIC",  "ordersStatuses");
        String ordersTopic = System.getenv().getOrDefault("ORDERS_TOPIC",  "orders");
        String productsTopic = System.getenv().getOrDefault("PRODUCTS_TOPIC",  "mysql.pizzashop.products");
        String enrichedOrderItemsTopic = System.getenv().getOrDefault("ENRICHED_ORDER_ITEMS_TOPIC",  "enriched-order-items");
        String enrichedOrdersTopic = System.getenv().getOrDefault("ENRICHED_ORDERS_TOPIC",  "enriched-orders");

        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Order.class));
        OrderItemWithContextSerde orderItemWithContextSerde = new OrderItemWithContextSerde();

        Serde<String> productKeySerde = DebeziumSerdes.payloadJson(String.class);
        productKeySerde.configure(Collections.emptyMap(), true);

        Serde<Product> productSerde = DebeziumSerdes.payloadJson(Product.class);
        productSerde.configure(Collections.singletonMap("from.field", "after"), false);

        final Serde<HydratedOrderItem> hydratedOrderItemsSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrderItem.class));
        final Serde<EnrichedOrder> enrichedOrdersSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(EnrichedOrder.class));
        final Serde<OrderStatus> orderStatusSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(OrderStatus.class));

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Order> orders = builder.stream(ordersTopic,
                Consumed.with(Serdes.String(), orderSerde));

        KTable<String, Product> products = builder.table(productsTopic,
                Consumed.with(productKeySerde, productSerde));

        KStream<String, OrderStatus> orderStatuses = builder.stream(orderStatusesTopic,
                Consumed.with(Serdes.String(), orderStatusSerde));

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
                    hydratedOrderItem.orderId = orderItem.orderId;
                    hydratedOrderItem.orderItem = orderItem.orderItem;
                    hydratedOrderItem.product = product;
                    hydratedOrderItem.createdAt = orderItem.createdAt;
                    return hydratedOrderItem;
                }, Joined.with(Serdes.String(), orderItemWithContextSerde, productSerde))
                .to(enrichedOrderItemsTopic, Produced.with(Serdes.String(), hydratedOrderItemsSerde));

        orders.join(orderStatuses, (value1, value2) -> {
                    EnrichedOrder enrichedOrder = new EnrichedOrder();
                    enrichedOrder.id = value1.id;
                    enrichedOrder.items = value1.items;
                    enrichedOrder.userId = value1.userId;
                    enrichedOrder.status = value2.status;
                    enrichedOrder.createdAt = value2.updatedAt;
                    enrichedOrder.price = value1.price;
                    return enrichedOrder;
                },
                JoinWindows.ofTimeDifferenceAndGrace(Duration.ofHours(2), Duration.ofHours(4)),
                StreamJoined.with(Serdes.String(), orderSerde, orderStatusSerde)
        ).to(enrichedOrdersTopic, Produced.with(Serdes.String(), enrichedOrdersSerde));

        Duration windowSize = Duration.ofSeconds(60);
        Duration advanceSize = Duration.ofSeconds(1);
        Duration gracePeriod = Duration.ofSeconds(60);
        TimeWindows timeWindow = TimeWindows.ofSizeAndGrace(windowSize, gracePeriod).advanceBy(advanceSize);

        orders.groupBy((key, value) -> "count", Grouped.with(Serdes.String(), orderSerde))
                .windowedBy(timeWindow)
                .count(Materialized.as("OrdersCountStore"));

        orders.groupBy((key, value) -> "count", Grouped.with(Serdes.String(), orderSerde))
                .windowedBy(timeWindow)
                .aggregate(
                        () -> 0.0, (key, value, aggregate) -> aggregate + value.price,
                        Materialized
                                .<String, Double, WindowStore<Bytes, byte[]>>as("RevenueStore")
                                .withValueSerde(Serdes.Double()));

        final Properties props = new Properties();

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, OrderItemWithContextSerde.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        org.apache.kafka.streams.Topology topology = builder.build(props);

        TopologyDescription describe = topology.describe();


        return topology;
    }
}