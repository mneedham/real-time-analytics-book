package pizzashop.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
import org.joda.time.DateTime;

import pizzashop.models.Order;
import pizzashop.models.OrdersSummary;
import pizzashop.models.PinotOrdersSummary;
import pizzashop.models.TimePeriod;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class OrdersQueries {

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

    public PinotOrdersSummary ordersSummary2() {
        ReadOnlyWindowStore<String, Long> ordersCounts = ordersCountsStore();

        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);
        Instant twoMinutesAgo = now.minusSeconds(120);
        long recentCount = getCount(ordersCounts, now, oneMinuteAgo);
        long previousCount = getCount(ordersCounts, oneMinuteAgo, twoMinutesAgo);

        TimePeriod currentTimePeriod = new TimePeriod(recentCount, 0);
        TimePeriod previousTimePeriod = new TimePeriod(previousCount, 0);
        return new PinotOrdersSummary(
                0, currentTimePeriod, previousTimePeriod
        );

    }

    private static long getCount(ReadOnlyWindowStore<String, Long> ordersCounts, Instant timeTo, Instant timeFrom) {
        try (WindowStoreIterator<Long> iterator = ordersCounts.backwardFetch("count", timeFrom, timeTo)) {
            if (iterator.hasNext()) {
                KeyValue<Long, Long> next = iterator.next();
                long windowTimestamp = next.key;
                System.out.println("Count of 'count' @ time " + windowTimestamp + " is " + next.value);
                return next.value;
            }
        }
        return 0;
    }

    private ReadOnlyWindowStore<String, Long> ordersCountsStore() {
        while (true) {
            try {
                return streams.store(
                        StoreQueryParameters.fromNameAndType("OrdersCountStore", QueryableStoreTypes.windowStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
    }


    private ReadOnlyKeyValueStore<String, Order> getOrders() {
        while (true) {
            try {
                return streams.store(
                        StoreQueryParameters.fromNameAndType("OrdersStore", QueryableStoreTypes.keyValueStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
    }
}