package pizzashop.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import pizzashop.models.Order;
import pizzashop.models.OrdersSummary;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InteractiveQueries {

    @Inject
    KafkaStreams streams;

    public OrdersSummary ordersSummary() {
        ReadOnlyKeyValueStore<String, Order> ordersStore = getOrders();
        OrdersSummary ordersSummary = new OrdersSummary(new DateTime());

        try (KeyValueIterator<String, Order> orders = ordersStore.all()) {
            while (orders.hasNext()) {
                KeyValue<String, Order> entry = orders.next();
                ordersSummary.add(entry.value);
            }
        }

        return ordersSummary;
    }

    private ReadOnlyKeyValueStore<String, Order> getOrders() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(
                        "OrdersStore", QueryableStoreTypes.keyValueStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
    }
}