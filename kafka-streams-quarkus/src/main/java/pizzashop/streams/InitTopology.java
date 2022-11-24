//package pizzashop.streams;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.serialization.Serde;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.common.utils.Bytes;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.StreamsConfig;
//import org.apache.kafka.streams.kstream.*;
//import org.apache.kafka.streams.state.KeyValueStore;
//import org.apache.kafka.streams.state.WindowStore;
//import pizzashop.deser.JsonDeserializer;
//import pizzashop.deser.JsonSerializer;
//import pizzashop.deser.OrderItemWithContextSerde;
//import pizzashop.models.Order;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.inject.Produces;
//import java.time.Duration;
//import java.util.Properties;
//
//@ApplicationScoped
//public class InitTopology {
//    @Produces
//    public org.apache.kafka.streams.Topology buildTopology() {
//        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Order.class));
//
//        StreamsBuilder builder = new StreamsBuilder();
//        KStream<String, Order> orders = builder.stream("orders", Consumed.with(Serdes.String(), orderSerde));
//
//        Materialized<String, Order, KeyValueStore<Bytes, byte[]>> ordersStore = Materialized.as("OrdersStore");
//        orders.toTable(ordersStore.withKeySerde(Serdes.String()).withValueSerde(orderSerde));
//
//        Duration windowSize = Duration.ofSeconds(60);
//        Duration advanceSize = Duration.ofSeconds(1);
//        Duration gracePeriod = Duration.ofSeconds(60);
//        TimeWindows timeWindow = TimeWindows.ofSizeAndGrace(windowSize, gracePeriod).advanceBy(advanceSize);
//
//        orders.groupBy((key, value) -> "count", Grouped.with(Serdes.String(), orderSerde))
//                .windowedBy(timeWindow)
//                .count(Materialized.as("OrdersCountStore"));
//
//        orders.groupBy((key, value) -> "count", Grouped.with(Serdes.String(), orderSerde))
//                .windowedBy(timeWindow)
//                .aggregate(
//                        () -> 0.0, (key, value, aggregate) -> aggregate + value.price,
//                        Materialized.<String, Double, WindowStore<Bytes, byte[]>>as("RevenueStore")
//                                .withValueSerde(Serdes.Double()));
//
//        final Properties props = new Properties();
//
//        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
//        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, OrderItemWithContextSerde.class.getName());
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        return builder.build(props);
//    }
//}