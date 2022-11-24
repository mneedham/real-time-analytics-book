package pizzashop.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OrdersSummary {
    private TimePeriod currentTimePeriod;
    private TimePeriod previousTimePeriod;


    public OrdersSummary(TimePeriod currentTimePeriod, TimePeriod previousTimePeriod) {
        this.currentTimePeriod = currentTimePeriod;
        this.previousTimePeriod = previousTimePeriod;
    }

    public TimePeriod getCurrentTimePeriod() {
        return currentTimePeriod;
    }

    public TimePeriod getPreviousTimePeriod() {
        return previousTimePeriod;
    }
}
