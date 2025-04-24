package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.model.Product;
import app.quantun.simpleapi.repository.CategoryRepository;
import app.quantun.simpleapi.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the ProductService.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product1;
    private Product product2;
    private List<Product> products;
    private Page<Product> productPage;

    @BeforeEach
    void setUp() {
        // Create test category
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Electronic devices");

        // Create test products
        product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");
        product1.setDescription("Powerful laptop");
        product1.setPrice(new BigDecimal("999.99"));
        product1.setCategory(category);

        product2 = new Product();
        product2.setId(2L);
        product2.setName("Smartphone");
        product2.setDescription("Latest smartphone");
        product2.setPrice(new BigDecimal("499.99"));
        product2.setCategory(category);

        products = Arrays.asList(product1, product2);
        productPage = new PageImpl<>(products);
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void shouldGetAllProductsWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getAllProducts(pageable);

        // Assert
        assertThat(result).isEqualTo(productPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Smartphone");
        verify(productRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should get all products without pagination")
    void shouldGetAllProductsWithoutPagination() {
        // Arrange
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertThat(result).isEqualTo(products);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        assertThat(result.get(1).getName()).isEqualTo("Smartphone");
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertThat(result).isEqualTo(product1);
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product not found")
    void shouldThrowEntityNotFoundExceptionWhenProductNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.getProductById(999L);
        });
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get products by category ID with pagination")
    void shouldGetProductsByCategoryIdWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getProductsByCategoryId(1L, pageable);

        // Assert
        assertThat(result).isEqualTo(productPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Smartphone");
        verify(productRepository, times(1)).findByCategoryId(1L, pageable);
    }

    @Test
    @DisplayName("Should get products by category ID without pagination")
    void shouldGetProductsByCategoryIdWithoutPagination() {
        // Arrange
        when(productRepository.findByCategoryId(1L)).thenReturn(products);

        // Act
        List<Product> result = productService.getProductsByCategoryId(1L);

        // Assert
        assertThat(result).isEqualTo(products);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        assertThat(result.get(1).getName()).isEqualTo("Smartphone");
        verify(productRepository, times(1)).findByCategoryId(1L);
    }

    @Test
    @DisplayName("Should get products by max price with pagination")
    void shouldGetProductsByMaxPriceWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal maxPrice = new BigDecimal("1000.00");
        when(productRepository.findByPriceLessThanEqual(maxPrice, pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getProductsByMaxPrice(maxPrice, pageable);

        // Assert
        assertThat(result).isEqualTo(productPage);
        assertThat(result.getContent()).hasSize(2);
        verify(productRepository, times(1)).findByPriceLessThanEqual(maxPrice, pageable);
    }

    @Test
    @DisplayName("Should get products by max price without pagination")
    void shouldGetProductsByMaxPriceWithoutPagination() {
        // Arrange
        BigDecimal maxPrice = new BigDecimal("1000.00");
        when(productRepository.findByPriceLessThanEqual(maxPrice)).thenReturn(products);

        // Act
        List<Product> result = productService.getProductsByMaxPrice(maxPrice);

        // Assert
        assertThat(result).isEqualTo(products);
        assertThat(result).hasSize(2);
        verify(productRepository, times(1)).findByPriceLessThanEqual(maxPrice);
    }

    @Test
    @DisplayName("Should get products by name with pagination")
    void shouldGetProductsByNameWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String name = "Laptop";
        when(productRepository.findByNameContainingIgnoreCase(name, pageable)).thenReturn(new PageImpl<>(List.of(product1)));

        // Act
        Page<Product> result = productService.getProductsByName(name, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findByNameContainingIgnoreCase(name, pageable);
    }

    @Test
    @DisplayName("Should get products by name without pagination")
    void shouldGetProductsByNameWithoutPagination() {
        // Arrange
        String name = "Laptop";
        when(productRepository.findByNameContainingIgnoreCase(name)).thenReturn(List.of(product1));

        // Act
        List<Product> result = productService.getProductsByName(name);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findByNameContainingIgnoreCase(name);
    }

    @Test
    @DisplayName("Should create product")
    void shouldCreateProduct() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setDescription("New Description");
        newProduct.setPrice(new BigDecimal("299.99"));
        newProduct.setCategory(category);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(newProduct);

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertThat(result).isEqualTo(newProduct);
        assertThat(result.getName()).isEqualTo("New Product");
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(newProduct);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when creating product with non-existent category")
    void shouldThrowEntityNotFoundExceptionWhenCreatingProductWithNonExistentCategory() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setDescription("New Description");
        newProduct.setPrice(new BigDecimal("299.99"));
        
        Category nonExistentCategory = new Category();
        nonExistentCategory.setId(999L);
        newProduct.setCategory(nonExistentCategory);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.createProduct(newProduct);
        });
        verify(categoryRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product")
    void shouldUpdateProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(new BigDecimal("399.99"));
        updatedProduct.setCategory(category);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.updateProduct(1L, updatedProduct);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("399.99"));
        assertThat(result.getCategory()).isEqualTo(category);
        verify(productRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent product")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(new BigDecimal("399.99"));

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.updateProduct(999L, updatedProduct);
        });
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating product with non-existent category")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingProductWithNonExistentCategory() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(new BigDecimal("399.99"));
        
        Category nonExistentCategory = new Category();
        nonExistentCategory.setId(999L);
        updatedProduct.setCategory(nonExistentCategory);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.updateProduct(1L, updatedProduct);
        });
        verify(productRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        doNothing().when(productRepository).delete(any(Product.class));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).delete(product1);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent product")
    void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentProduct() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.deleteProduct(999L);
        });
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).delete(any(Product.class));
    }
}