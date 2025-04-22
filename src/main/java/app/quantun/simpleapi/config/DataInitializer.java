package app.quantun.simpleapi.config;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.model.Product;
import app.quantun.simpleapi.repository.CategoryRepository;
import app.quantun.simpleapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuration class to initialize sample data for testing.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Initialize sample data for testing.
     * This bean is only active in the "default" profile (not in "test" profile).
     */
    @Bean
    @Profile("!test")
    public CommandLineRunner initData() {
        return args -> {
            log.info("Initializing sample data...");
            
            // Create categories
            Category electronics = new Category();
            electronics.setName("Electronics");
            electronics.setDescription("Electronic devices and gadgets");
            
            Category clothing = new Category();
            clothing.setName("Clothing");
            clothing.setDescription("Apparel and fashion items");
            
            Category books = new Category();
            books.setName("Books");
            books.setDescription("Books and literature");
            
            List<Category> categories = categoryRepository.saveAll(List.of(electronics, clothing, books));
            log.info("Created {} categories", categories.size());
            
            // Create products
            Product laptop = new Product();
            laptop.setName("Laptop Pro");
            laptop.setDescription("High-performance laptop for professionals");
            laptop.setPrice(new BigDecimal("1299.99"));
            laptop.setImageUrl("https://example.com/images/laptop.jpg");
            laptop.setCategory(electronics);
            
            Product smartphone = new Product();
            smartphone.setName("Smartphone X");
            smartphone.setDescription("Latest smartphone with advanced features");
            smartphone.setPrice(new BigDecimal("899.99"));
            smartphone.setImageUrl("https://example.com/images/smartphone.jpg");
            smartphone.setCategory(electronics);
            
            Product tShirt = new Product();
            tShirt.setName("Cotton T-Shirt");
            tShirt.setDescription("Comfortable cotton t-shirt");
            tShirt.setPrice(new BigDecimal("19.99"));
            tShirt.setImageUrl("https://example.com/images/tshirt.jpg");
            tShirt.setCategory(clothing);
            
            Product jeans = new Product();
            jeans.setName("Denim Jeans");
            jeans.setDescription("Classic denim jeans");
            jeans.setPrice(new BigDecimal("49.99"));
            jeans.setImageUrl("https://example.com/images/jeans.jpg");
            jeans.setCategory(clothing);
            
            Product novel = new Product();
            novel.setName("The Great Novel");
            novel.setDescription("Bestselling fiction novel");
            novel.setPrice(new BigDecimal("14.99"));
            novel.setImageUrl("https://example.com/images/novel.jpg");
            novel.setCategory(books);
            
            List<Product> products = productRepository.saveAll(List.of(laptop, smartphone, tShirt, jeans, novel));
            log.info("Created {} products", products.size());
            
            log.info("Sample data initialization completed");
        };
    }
}