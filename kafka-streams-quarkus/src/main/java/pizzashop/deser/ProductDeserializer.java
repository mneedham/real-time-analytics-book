package pizzashop.deser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import pizzashop.models.Product;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProductDeserializer implements Deserializer<Product> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ProductDeserializer() {
    }


    @Override
    public Product deserialize(final String topic, final byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            Map<String, Object> o = OBJECT_MAPPER.readValue(data, Map.class);

            if(o.get("payload") == null) {
                return null;
            }

            Map<String, Object> payload = (Map<String, Object>) o.get("payload");
            Product product = new Product();
            product.id = String.valueOf(payload.get("id"));
            product.name = String.valueOf(payload.get("name"));
            product.price = (double) payload.get("price");
            product.category = String.valueOf(payload.get("category"));
            product.image = String.valueOf(payload.get("image"));
            product.description = String.valueOf(payload.get("description"));
            return product;
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
