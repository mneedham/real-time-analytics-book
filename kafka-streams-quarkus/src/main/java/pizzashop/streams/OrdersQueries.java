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
        ReadOnlyWindowStore<String, Long> countStore = ordersCountsStore();
        ReadOnlyWindowStore<String, Double> revenueStore = revenueStore();

        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);
        Instant twoMinutesAgo = now.minusSeconds(120);

        long recentCount = getCount(countStore, now, oneMinuteAgo);
        long previousCount = getCount(countStore, oneMinuteAgo, twoMinutesAgo);

        double recentRevenue = getRevenue(revenueStore, now, oneMinuteAgo);
        double previousRevenue = getRevenue(revenueStore, oneMinuteAgo, twoMinutesAgo);

        TimePeriod currentTimePeriod = new TimePeriod(recentCount, recentRevenue);
        TimePeriod previousTimePeriod = new TimePeriod(previousCount, previousRevenue);
        return new PinotOrdersSummary(
                0, currentTimePeriod, previousTimePeriod
        );

    }

    private static double getRevenue(ReadOnlyWindowStore<String, Double> revenue, Instant timeTo, Instant timeFrom) {
        try (WindowStoreIterator<Double> iterator = revenue.backwardFetch("count", timeFrom, timeTo)) {
            if (iterator.hasNext()) {
                KeyValue<Long, Double> next = iterator.next();
                long windowTimestamp = next.key;
                System.out.println("Count of 'count' @ time " + windowTimestamp + " is " + next.value);
                return next.value;
            }
        }
        return 0;
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

    private ReadOnlyWindowStore<String, Double> revenueStore() {
        while (true) {
            try {
                return streams.store(
                        StoreQueryParameters.fromNameAndType("RevenueStore", QueryableStoreTypes.windowStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
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