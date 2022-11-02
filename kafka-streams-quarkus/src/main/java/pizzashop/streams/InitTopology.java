//package pizzashop.streams;
//
//import org.apache.kafka.common.serialization.Serde;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.common.utils.Bytes;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.kstream.Consumed;
//import org.apache.kafka.streams.kstream.KStream;
//import org.apache.kafka.streams.kstream.Materialized;
//import org.apache.kafka.streams.kstream.TimeWindows;
//import org.apache.kafka.streams.state.KeyValueStore;
//import pizzashop.deser.JsonDeserializer;
//import pizzashop.deser.JsonSerializer;
//import pizzashop.models.Order;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.enterprise.inject.Produces;
//import java.time.Duration;
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
//        TimeWindows tumblingWindow = TimeWindows.of(windowSize);
////        orders.groupByKey().windowedBy(tumblingWindow).count
//
//        return builder.build();
//    }
//}