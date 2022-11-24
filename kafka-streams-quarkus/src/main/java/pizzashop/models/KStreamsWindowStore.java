package pizzashop.models;

import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

import java.time.Instant;

public class KStreamsWindowStore<T> {
    private final ReadOnlyWindowStore<String, T> store;

    public KStreamsWindowStore(ReadOnlyWindowStore<String, T> store) {
        this.store = store;
    }

    public T firstEntry(Instant from, Instant to) {
        try (WindowStoreIterator<T> iterator = store.fetch("count", from, to)) {
            if (iterator.hasNext()) {
                return iterator.next().value;
            }
        }
        throw new RuntimeException("No entries found in store between " + from + " and " + to);
    }
}
