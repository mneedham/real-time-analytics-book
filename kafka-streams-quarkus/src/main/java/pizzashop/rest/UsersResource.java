package pizzashop.rest;

import org.apache.pinot.client.Connection;
import org.apache.pinot.client.ConnectionFactory;
import org.apache.pinot.client.ResultSet;
import org.apache.pinot.client.ResultSetGroup;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import pizzashop.models.*;
import pizzashop.streams.OrdersQueries;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;

@ApplicationScoped
@Path("/users")
public class UsersResource {
    private Connection connection = ConnectionFactory.fromHostList(System.getenv().getOrDefault("PINOT_BROKER",  "localhost:8099"));


    @GET
    @Path("/{userId}/orders")
    public Response userOrders(@PathParam("userId") String userId) {
        String query = DSL.using(SQLDialect.POSTGRES)
                .select(
                        field("id"),
                        field("price"),
                        field("ToDateTime(ts, 'YYYY-MM-dd HH:mm:ss')").as("ts")
                )
                .from("orders_enriched")
                .where(field("userId").eq(field("'" + userId + "'")))
                .orderBy(field("ts").desc())
                .limit(DSL.inline(50))
                .getSQL();
        
        ResultSet resultSet = runQuery(connection, query);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < resultSet.getRowCount(); index++) {
            rows.add(Map.of(
                    "id", resultSet.getString(index, 0),
                    "price", resultSet.getDouble(index, 1),
                    "ts", resultSet.getString(index, 2)
            ));
        }

        return Response.ok(rows).build();
    }

    @GET
    @Path("/")
    public Response allUsers() {
        String query = DSL.using(SQLDialect.POSTGRES)
                .select(
                        field("userId"),
                        field("ts")
                )
                .from("orders")
                .orderBy(field("ts").desc())
                .limit(DSL.inline(50))
                .getSQL();

        ResultSet resultSet = runQuery(connection, query);

        Stream<Map<String, Object>> rows = IntStream.range(0, resultSet.getRowCount())
                .mapToObj(index -> Map.of("userId", resultSet.getString(index, 0)));

        return Response.ok(rows).build();
    }

    private static ResultSet runQuery(Connection connection, String query) {
        ResultSetGroup resultSetGroup = connection.execute(query);
        return resultSetGroup.getResultSet(0);
    }
}
