package pizzashop;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.deser.OrderItemWithContextSerde;
import pizzashop.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


public class OrderItemsProductsJoin {

    public static void main(String[] args) {
        final Properties props = new Properties();

        final Serde<OrderItem> orderItemSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(OrderItem.class));

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "pizzashop-orderitems21" + System.currentTimeMillis());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("BOOTSTRAP_SERVER", "localhost:29092"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, OrderItemWithContextSerde.class.getName());

        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Order.class));
        final Serde<Product> productSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Product.class));
        final Serde<HydratedOrderItem> hydratedOrderItemsSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrderItem.class));
        final Serde<HydratedOrder> hydratedOrdersSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrder.class));
        final Serde<CompleteOrder> completeOrderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(CompleteOrder.class));

        String ordersTopic = System.getenv().getOrDefault("ORDERS_TOPIC",  "orders-multi9");
        String productsTopic = System.getenv().getOrDefault("PRODUCTS_TOPIC",  "products-multi9");
        String enrichedOrdersTopic = System.getenv().getOrDefault("ENRICHED_ORDERS_TOPIC",  "enriched-orders-multi10");

        KStream<String, Order> orders = builder.stream(ordersTopic, Consumed.with(Serdes.String(), orderSerde));
        KTable<String, Product> products = builder.table(productsTopic, Consumed.with(Serdes.String(), productSerde));

        KStream<String, OrderItemWithContext> orderItems = orders.flatMap((key, value) -> {
            List<KeyValue<String, OrderItemWithContext>> result = new ArrayList<>();
            for (OrderItem item : value.items) {
                OrderItemWithContext orderItemWithContext = new OrderItemWithContext();
                orderItemWithContext.orderId = value.id;
                orderItemWithContext.orderItem = item;
                result.add(new KeyValue<>(String.valueOf(item.productId), orderItemWithContext));
            }
            return result;
        });

        KTable<String, HydratedOrder> hydratedOrders = orderItems.join(products, (orderItem, product) -> {
                    HydratedOrderItem hydratedOrderItem = new HydratedOrderItem();
                    hydratedOrderItem.orderId = orderItem.orderId;
                    hydratedOrderItem.orderItem = orderItem.orderItem;
                    hydratedOrderItem.product = product;
                    return hydratedOrderItem;
                })
                .groupBy((key, value) -> value.orderId, Grouped.with(Serdes.String(), hydratedOrderItemsSerde))
                .aggregate(HydratedOrder::new, (key, value, aggregate) -> {
                    aggregate.addOrderItem(value);
                    return aggregate;
                }, Materialized.with(Serdes.String(), hydratedOrdersSerde));

        orders.toTable().join(hydratedOrders, (value1, value2) -> {
            CompleteOrder completeOrder = new CompleteOrder();
            completeOrder.id = value1.id;
            completeOrder.status = value1.status;
            completeOrder.userId = value1.userId;
            completeOrder.createdAt = value1.createdAt;
            completeOrder.price = value1.price;

            completeOrder.items = value2.orderItems.stream().map(orderItem -> {
                CompleteOrderItem completeOrderItem = new CompleteOrderItem();
                completeOrderItem.product = orderItem.product;
                completeOrderItem.quantity = orderItem.orderItem.quantity;
                return completeOrderItem;
            }).collect(Collectors.toList());
            return completeOrder;
        }).toStream().to(enrichedOrdersTopic, Produced.with(Serdes.String(), completeOrderSerde));

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("pizzashop-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }


}
