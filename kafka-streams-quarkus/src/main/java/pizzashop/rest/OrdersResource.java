package pizzashop.rest;

import org.apache.pinot.client.Connection;
import org.apache.pinot.client.ConnectionFactory;
import org.apache.pinot.client.ResultSet;
import org.apache.pinot.client.ResultSetGroup;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import pizzashop.models.OrdersSummary;
import pizzashop.models.SummaryRow;
import pizzashop.models.TimePeriod;
import pizzashop.streams.OrdersQueries;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.*;

@ApplicationScoped
@Path("/orders")
public class OrdersResource {
    @Inject
    OrdersQueries ordersQueries;

    private Connection connection = ConnectionFactory.fromHostList(System.getenv().getOrDefault("PINOT_BROKER",  "localhost:8099"));

//    @GET
//    @Path("/overview")
//    public Response overview() {
//        OrdersSummary ordersSummary = ordersQueries.ordersSummary();
//        return Response.ok(ordersSummary).build();
//    }

    @GET
    @Path("/overview")
    public Response overview() {
        OrdersSummary ordersSummary = ordersQueries.ordersSummary();
        return Response.ok(ordersSummary).build();
    }

    @GET
    @Path("/overview2")
    public Response overview2() {
        ResultSet resultSet = runQuery(connection, "select count(*) from orders limit 10");

        String query = DSL.using(SQLDialect.POSTGRES)
                .select(
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
        OrdersSummary ordersSummary = new OrdersSummary(
                currentTimePeriod, previousTimePeriod
        );

        return Response.ok(ordersSummary).build();
    }

    @GET
    @Path("/ordersPerMinute")
    public Response ordersPerMinute() {
        String query = DSL.using(SQLDialect.POSTGRES)
                .select(
                        field("ToDateTime(DATETRUNC('MINUTE', ts), 'yyyy-MM-dd HH:mm:ss')")
                                .as("dateMin"),
                        count(field("*")),
                        sum(field("price").coerce(Long.class))
                )
                .from("orders")
                .groupBy(field("dateMin"))
                .orderBy(field("dateMin"))
                .$where(field("dateMin").greaterThan(field("ago('PT1H')")))
                .$limit(DSL.inline(60))
                .getSQL();

        ResultSet summaryResults = runQuery(connection, query);

        int rowCount = summaryResults.getRowCount();

        List<SummaryRow> rows = new ArrayList<>();
        for (int index = 0; index < rowCount; index++) {
            rows.add(new SummaryRow(
                    summaryResults.getString(index, 0),
                    summaryResults.getLong(index, 1),
                    summaryResults.getDouble(index, 2)
            ));
        }

        return Response.ok(rows).build();
    }

    private static ResultSet runQuery(Connection connection, String query) {
        ResultSetGroup resultSetGroup = connection.execute(query);
        return resultSetGroup.getResultSet(0);
    }
}
