package app.quantun.simpleapi.repository;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the ProductRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setup() {
        // Clear the repositories
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test categories
        category1 = new Category();
        category1.setName("Electronics");
        category1.setDescription("Electronic devices");
        category1 = categoryRepository.save(category1);

        category2 = new Category();
        category2.setName("Books");
        category2.setDescription("Reading materials");
        category2 = categoryRepository.save(category2);
    }

    @Test
    @DisplayName("Should save a product")
    void shouldSaveProduct() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setImageUrl("http://example.com/image.jpg");
        product.setCategory(category1);

        // Act
        Product savedProduct = productRepository.save(product);

        // Assert
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        assertThat(savedProduct.getDescription()).isEqualTo("Test Description");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(savedProduct.getImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(savedProduct.getCategory()).isEqualTo(category1);
    }

    @Test
    @DisplayName("Should find a product by ID")
    void shouldFindProductById() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(category1);
        Product savedProduct = productRepository.save(product);

        // Act
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

        // Assert
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
        assertThat(foundProduct.get().getCategory()).isEqualTo(category1);
    }

    @Test
    @DisplayName("Should find products by category")
    void shouldFindProductsByCategory() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("99.99"));
        product1.setCategory(category1);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("199.99"));
        product2.setCategory(category1);
        productRepository.save(product2);

        Product product3 = new Product();
        product3.setName("Product 3");
        product3.setDescription("Description 3");
        product3.setPrice(new BigDecimal("29.99"));
        product3.setCategory(category2);
        productRepository.save(product3);

        // Act
        List<Product> products = productRepository.findByCategory(category1);

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder("Product 1", "Product 2");
    }

    @Test
    @DisplayName("Should find products by category with pagination")
    void shouldFindProductsByCategoryWithPagination() {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(new BigDecimal(i * 10));
            product.setCategory(i % 2 == 0 ? category1 : category2);
            productRepository.save(product);
        }

        // Act
        Pageable pageable = PageRequest.of(0, 2, Sort.by("price").ascending());
        Page<Product> productPage = productRepository.findByCategory(category1, pageable);

        // Assert
        assertThat(productPage.getContent()).hasSize(2);
        assertThat(productPage.getTotalElements()).isEqualTo(5);
        assertThat(productPage.getTotalPages()).isEqualTo(3);
        assertThat(productPage.getContent()).extracting(Product::getName)
                .containsExactly("Product 2", "Product 4");
    }

    @Test
    @DisplayName("Should find products by category ID")
    void shouldFindProductsByCategoryId() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("99.99"));
        product1.setCategory(category1);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("199.99"));
        product2.setCategory(category1);
        productRepository.save(product2);

        Product product3 = new Product();
        product3.setName("Product 3");
        product3.setDescription("Description 3");
        product3.setPrice(new BigDecimal("29.99"));
        product3.setCategory(category2);
        productRepository.save(product3);

        // Act
        List<Product> products = productRepository.findByCategoryId(category1.getId());

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder("Product 1", "Product 2");
    }

    @Test
    @DisplayName("Should find products by category ID with pagination")
    void shouldFindProductsByCategoryIdWithPagination() {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(new BigDecimal(i * 10));
            product.setCategory(i % 2 == 0 ? category1 : category2);
            productRepository.save(product);
        }

        // Act
        Pageable pageable = PageRequest.of(0, 2, Sort.by("price").ascending());
        Page<Product> productPage = productRepository.findByCategoryId(category1.getId(), pageable);

        // Assert
        assertThat(productPage.getContent()).hasSize(2);
        assertThat(productPage.getTotalElements()).isEqualTo(5);
        assertThat(productPage.getTotalPages()).isEqualTo(3);
        assertThat(productPage.getContent()).extracting(Product::getName)
                .containsExactly("Product 2", "Product 4");
    }

    @Test
    @DisplayName("Should find products by price less than or equal")
    void shouldFindProductsByPriceLessThanEqual() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("50.00"));
        product1.setCategory(category1);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("100.00"));
        product2.setCategory(category1);
        productRepository.save(product2);

        Product product3 = new Product();
        product3.setName("Product 3");
        product3.setDescription("Description 3");
        product3.setPrice(new BigDecimal("150.00"));
        product3.setCategory(category2);
        productRepository.save(product3);

        // Act
        List<Product> products = productRepository.findByPriceLessThanEqual(new BigDecimal("100.00"));

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder("Product 1", "Product 2");
    }

    @Test
    @DisplayName("Should find products by price less than or equal with pagination")
    void shouldFindProductsByPriceLessThanEqualWithPagination() {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(new BigDecimal(i * 10));
            product.setCategory(i % 2 == 0 ? category1 : category2);
            productRepository.save(product);
        }

        // Act
        Pageable pageable = PageRequest.of(0, 3, Sort.by("price").ascending());
        Page<Product> productPage = productRepository.findByPriceLessThanEqual(new BigDecimal("50"), pageable);

        // Assert
        assertThat(productPage.getContent()).hasSize(3);
        assertThat(productPage.getTotalElements()).isEqualTo(5);
        assertThat(productPage.getTotalPages()).isEqualTo(2);
        assertThat(productPage.getContent()).extracting(Product::getName)
                .containsExactly("Product 1", "Product 2", "Product 3");
    }

    @Test
    @DisplayName("Should find products by name containing ignore case")
    void shouldFindProductsByNameContainingIgnoreCase() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Laptop Dell XPS");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("1500.00"));
        product1.setCategory(category1);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Laptop HP Spectre");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("1200.00"));
        product2.setCategory(category1);
        productRepository.save(product2);

        Product product3 = new Product();
        product3.setName("Desktop PC");
        product3.setDescription("Description 3");
        product3.setPrice(new BigDecimal("1000.00"));
        product3.setCategory(category1);
        productRepository.save(product3);

        // Act
        List<Product> products = productRepository.findByNameContainingIgnoreCase("laptop");

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder("Laptop Dell XPS", "Laptop HP Spectre");
    }

    @Test
    @DisplayName("Should find products by name containing ignore case with pagination")
    void shouldFindProductsByNameContainingIgnoreCaseWithPagination() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Laptop Dell XPS");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("1500.00"));
        product1.setCategory(category1);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Laptop HP Spectre");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("1200.00"));
        product2.setCategory(category1);
        productRepository.save(product2);

        Product product3 = new Product();
        product3.setName("Laptop Lenovo ThinkPad");
        product3.setDescription("Description 3");
        product3.setPrice(new BigDecimal("1100.00"));
        product3.setCategory(category1);
        productRepository.save(product3);

        Product product4 = new Product();
        product4.setName("Desktop PC");
        product4.setDescription("Description 4");
        product4.setPrice(new BigDecimal("1000.00"));
        product4.setCategory(category1);
        productRepository.save(product4);

        // Act
        Pageable pageable = PageRequest.of(0, 2, Sort.by("price").descending());
        Page<Product> productPage = productRepository.findByNameContainingIgnoreCase("laptop", pageable);

        // Assert
        assertThat(productPage.getContent()).hasSize(2);
        assertThat(productPage.getTotalElements()).isEqualTo(3);
        assertThat(productPage.getTotalPages()).isEqualTo(2);
        assertThat(productPage.getContent()).extracting(Product::getName)
                .containsExactly("Laptop Dell XPS", "Laptop HP Spectre");
    }

    @Test
    @DisplayName("Should update a product")
    void shouldUpdateProduct() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(category1);
        Product savedProduct = productRepository.save(product);

        // Act
        savedProduct.setName("Updated Product");
        savedProduct.setDescription("Updated Description");
        savedProduct.setPrice(new BigDecimal("149.99"));
        savedProduct.setCategory(category2);
        Product updatedProduct = productRepository.save(savedProduct);

        // Assert
        assertThat(updatedProduct.getId()).isEqualTo(savedProduct.getId());
        assertThat(updatedProduct.getName()).isEqualTo("Updated Product");
        assertThat(updatedProduct.getDescription()).isEqualTo("Updated Description");
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        assertThat(updatedProduct.getCategory()).isEqualTo(category2);
    }

    @Test
    @DisplayName("Should delete a product")
    void shouldDeleteProduct() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(category1);
        Product savedProduct = productRepository.save(product);

        // Act
        productRepository.delete(savedProduct);
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

        // Assert
        assertThat(foundProduct).isEmpty();
    }
}