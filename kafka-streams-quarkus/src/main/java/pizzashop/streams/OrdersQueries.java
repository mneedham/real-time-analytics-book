package pizzashop.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;

import pizzashop.models.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class OrdersQueries {
    @Inject
    KafkaStreams streams;

    public OrdersSummary ordersSummary() {
        KStreamsWindowStore<Long> countStore = new KStreamsWindowStore<>(ordersCountsStore());
        KStreamsWindowStore<Double> revenueStore = new KStreamsWindowStore<>(revenueStore());

        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);
        Instant twoMinutesAgo = now.minusSeconds(120);

        long recentCount = countStore.firstEntry(oneMinuteAgo, now);
        double recentRevenue = revenueStore.firstEntry(oneMinuteAgo, now);

        long  previousCount = countStore.firstEntry(twoMinutesAgo, oneMinuteAgo);
        double previousRevenue = revenueStore.firstEntry(twoMinutesAgo, oneMinuteAgo);

        TimePeriod currentTimePeriod = new TimePeriod(recentCount, recentRevenue);
        TimePeriod previousTimePeriod = new TimePeriod(previousCount, previousRevenue);
        return new OrdersSummary(
                currentTimePeriod, previousTimePeriod
        );
    }

    private ReadOnlyWindowStore<String, Double> revenueStore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(
                        "RevenueStore", QueryableStoreTypes.windowStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
    }

    private ReadOnlyWindowStore<String, Long> ordersCountsStore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(
                        "OrdersCountStore", QueryableStoreTypes.windowStore())
                );
            } catch (InvalidStateStoreException e) {
                System.out.println("e = " + e);
            }
        }
    }
}