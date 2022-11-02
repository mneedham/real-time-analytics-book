package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

@RegisterForReflection
public class PinotOrdersSummary {
    private int totalOrders = 0;

    private TimePeriod currentTimePeriod = new TimePeriod();
    private TimePeriod previousTimePeriod = new TimePeriod();


    public PinotOrdersSummary(int totalOrders, TimePeriod currentTimePeriod, TimePeriod previousTimePeriod) {
        this.totalOrders = totalOrders;
        this.currentTimePeriod = currentTimePeriod;
        this.previousTimePeriod = previousTimePeriod;
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
