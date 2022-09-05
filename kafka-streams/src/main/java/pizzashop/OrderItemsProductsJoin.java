package pizzashop;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.deser.OrderItemWithOrderIdSerde;
import pizzashop.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class OrderItemsProductsJoin {

    public static void main(String[] args) {
        final Properties props = new Properties();

        final Serde<OrderItem> orderItemSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(OrderItem.class));

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "pizzashop-orderitems21" + System.currentTimeMillis());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("BOOTSTRAP_SERVER", "localhost:29092"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Bytes().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, OrderItemWithOrderIdSerde.class.getName());

        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Order.class));
        final KStream<Bytes, Order> orders = builder.stream("orders5",
                Consumed.with(Serdes.Bytes(), orderSerde));

        KStream<Bytes, OrderItemWithOrderId> orderItems = orders.flatMap((KeyValueMapper<Bytes, Order, Iterable<KeyValue<Bytes, OrderItemWithOrderId>>>) (bytes, order) -> {
            List<KeyValue<Bytes, OrderItemWithOrderId>> result = new ArrayList<>();

            for (OrderItem item : order.items) {
                OrderItemWithOrderId orderItemWithOrderId = new OrderItemWithOrderId();
                orderItemWithOrderId.orderId = order.id;
                orderItemWithOrderId.price = item.price;
                orderItemWithOrderId.quantity = item.quantity;
                orderItemWithOrderId.productId = item.productId;
                result.add(new KeyValue(new Bytes(String.valueOf(item.productId).getBytes()), orderItemWithOrderId));
            }
            return result;
        });

        final Serde<Product> productSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Product.class));
        final KTable<Bytes, Product> products = builder.table("products6",
                Consumed.with(Serdes.Bytes(), productSerde));

        KStream<Bytes, HydratedOrderItem> enrichedOrderItems = orderItems.leftJoin(products, (orderItem, product) -> {
            HydratedOrderItem hydratedOrderItem = new HydratedOrderItem();
            hydratedOrderItem.product = product;

            OrderItem item = new OrderItem();
            item.productId = orderItem.productId;
            item.price = orderItem.price;
            item.quantity = orderItem.quantity;

            hydratedOrderItem.orderId = orderItem.orderId;
            hydratedOrderItem.orderItem = item;

            return hydratedOrderItem;
        });

        final Serde<HydratedOrderItem> hydratedOrderItemsSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrderItem.class));

//        enrichedOrderItems.to("enriched-order-items5", Produced.with(Serdes.Bytes(), hydratedOrderItemsSerde));

        final Serde<HydratedOrder> hydratedOrdersSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(HydratedOrder.class));

        KTable<Bytes, HydratedOrder> hydratedOrders = enrichedOrderItems
                .groupBy((key, value) -> new Bytes(value.orderId.getBytes()),
                        Grouped.with(Serdes.Bytes(), hydratedOrderItemsSerde))
                .aggregate(HydratedOrder::new, (key, value, aggregate) -> {
                    aggregate.addOrderItem(value);
                    return aggregate;
                }, Materialized.with(Serdes.Bytes(), hydratedOrdersSerde));

        orders.peek((key, value) -> System.out.println("key = " + key + ", value = " + value));

        KStream<Bytes, Order> rekeyedOrders = orders.selectKey((key, value) -> new Bytes(value.id.getBytes()));

        hydratedOrders.toStream().peek((key, value) -> System.out.println("key = " + key + ", value = " + value));
//
//        hydratedOrders.toStream().to("rogue-orders6", Produced.with(Serdes.Bytes(), hydratedOrdersSerde));

        final Serde<CompleteOrder> completeOrderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(CompleteOrder.class));

        rekeyedOrders.leftJoin(hydratedOrders, (value1, value2) -> {
            CompleteOrder completeOrder = new CompleteOrder();
            completeOrder.id = value1.id;
            completeOrder.status = value1.status;
            completeOrder.userId = value1.userId;
            completeOrder.createdAt = value1.createdAt;

            if(value2 != null) {
                completeOrder.orderItems = value2.orderItems;
            }
            return completeOrder;
        })
                .peek((key, value) -> System.out.println("key = " + key + ", value = " + value))
                .to("enriched-orders6", Produced.with(Serdes.Bytes(), completeOrderSerde));

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
