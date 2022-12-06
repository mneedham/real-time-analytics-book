package pizzashop.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// POJO classes
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    public Product() {

    }

    public String id;
    public String name;
    public String description;
    public String category;
    public String image;
    public double price;
}
