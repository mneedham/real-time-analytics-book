package pizzashop;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import pizzashop.deser.JsonDeserializer;
import pizzashop.deser.JsonSerializer;
import pizzashop.models.Order;
import pizzashop.models.Product;
import pizzashop.models.UpdatedOrder;


public class OrdersProductsJoin {

    public static void main(String[] args) {
        final Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "pizzashop");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("BOOTSTRAP_SERVER", "localhost:29092"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final StreamsBuilder builder = new StreamsBuilder();

        final Serde<Order> orderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Order.class));
        final KStream<Bytes, Order> orders = builder.stream("orders",
                Consumed.with(Serdes.Bytes(), orderSerde));

        final Serde<Product> productSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(Product.class));
        final KTable<Bytes, Product> products = builder.table("products",
                Consumed.with(Serdes.Bytes(), productSerde));

        final KStream<Bytes, UpdatedOrder> enrichedOrders = orders
                .leftJoin(products, (order, product) -> {
                    final UpdatedOrder uo = new UpdatedOrder();
                    uo.order = order;

                    if(product != null ) {
                        uo.product = product;
                    }
                    return uo;
                });

        final Serde<UpdatedOrder> updatedOrderSerde = Serdes.serdeFrom(new JsonSerializer<>(),
                new JsonDeserializer<>(UpdatedOrder.class));

        enrichedOrders.to("enriched-orders", Produced.with(Serdes.Bytes(), updatedOrderSerde));

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
