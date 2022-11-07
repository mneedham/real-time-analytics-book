package pizzashop.rest;

import org.apache.pinot.client.Connection;
import org.apache.pinot.client.ConnectionFactory;
import org.apache.pinot.client.ResultSet;
import org.apache.pinot.client.ResultSetGroup;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import pizzashop.models.OrdersSummary;
import pizzashop.models.PinotOrdersSummary;
import pizzashop.models.TimePeriod;
import pizzashop.streams.OrdersQueries;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.jooq.impl.DSL.*;

@ApplicationScoped
@Path("/orders")
public class OrdersResource {
    @Inject
    OrdersQueries ordersQueries;

    private Connection connection = ConnectionFactory.fromHostList("localhost:8099");

    @GET
    @Path("/overview")
    public Response overview() {
        OrdersSummary ordersSummary = ordersQueries.ordersSummary();
        return Response.ok(ordersSummary).build();
    }

    @GET
    @Path("/overview3")
    public Response overview3() {
        PinotOrdersSummary ordersSummary = ordersQueries.ordersSummary2();
        return Response.ok(ordersSummary).build();
    }

    @GET
    @Path("/overview2")
    public Response overview2() {
        ResultSet resultSet = runQuery(connection, "select count(*) from orders limit 10");
        int totalOrders = resultSet.getInt(0);

        String query = DSL.using(SQLDialect.POSTGRES).select(
                count()
                        .filterWhere("ts > ago('PT1M')")
                        .as("events1Min"),

                count()
                        .filterWhere("ts <= ago('PT1M') AND ts > ago('PT2M')")
                        .as("events1Min2Min"),

                sum(field("price").coerce(Long.class))
                        .filterWhere("ts > ago('PT1M')")
                        .as("total1Min"),

                sum(field("price").coerce(Long.class))
                        .filterWhere("ts <= ago('PT1M') AND ts > ago('PT2M')")
                        .as("total1Min2Min")

        ).from("orders").getSQL();

        ResultSet summaryResults = runQuery(connection, query);

        TimePeriod currentTimePeriod = new TimePeriod(
                summaryResults.getLong(0, 0), summaryResults.getDouble(0, 2));
        TimePeriod previousTimePeriod = new TimePeriod(
                summaryResults.getLong(0, 1), summaryResults.getDouble(0, 3));
        PinotOrdersSummary ordersSummary = new PinotOrdersSummary(
                totalOrders, currentTimePeriod, previousTimePeriod
        );

        return Response.ok(ordersSummary).build();
    }

    private static ResultSet runQuery(Connection connection, String query) {
        ResultSetGroup resultSetGroup = connection.execute(query);
        return resultSetGroup.getResultSet(0);
    }
}
