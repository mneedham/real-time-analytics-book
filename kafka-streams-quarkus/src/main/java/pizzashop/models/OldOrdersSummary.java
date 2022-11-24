package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

@RegisterForReflection
public class OldOrdersSummary {
    private final long timeWindow;
    private int totalOrders = 0;

    private TimePeriod currentTimePeriod = new TimePeriod();
    private TimePeriod previousTimePeriod = new TimePeriod();

    private DateTime currentTime;

    public OldOrdersSummary(DateTime currentTime) {
        this.currentTime = currentTime;
        this.timeWindow = TimeUnit.SECONDS.toMillis(60);
    }

    public void add(Order order) {
        DateTime orderTime = DateTime.parse(order.createdAt);

        if (orderTime.getMillis() > currentTime.minus(timeWindow).getMillis()) {
            currentTimePeriod.addOrder(order);
        }

        if (orderTime.getMillis() < currentTime.minus(timeWindow).getMillis() &&
                orderTime.getMillis() > currentTime.minus(timeWindow * 2).getMillis()) {
            previousTimePeriod.addOrder(order);
        }

        totalOrders += 1;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public TimePeriod getCurrentTimePeriod() {
        return currentTimePeriod;
    }

    public TimePeriod getPreviousTimePeriod() {
        return previousTimePeriod;
    }
}
