package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.model.Product;
import app.quantun.simpleapi.repository.CategoryRepository;
import app.quantun.simpleapi.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service class for Product operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Get all products with pagination.
     *
     * @param pageable pagination information
     * @return page of products
     */
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * Get all products without pagination (legacy method).
     *
     * @return list of all products
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Get product by ID.
     *
     * @param id the product ID
     * @return the product with the given ID
     * @throws EntityNotFoundException if product not found
     */
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    /**
     * Get products by category ID with pagination.
     *
     * @param categoryId the category ID
     * @param pageable   pagination information
     * @return page of products in the given category
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    /**
     * Get products by category ID without pagination (legacy method).
     *
     * @param categoryId the category ID
     * @return list of products in the given category
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    /**
     * Get products by price less than or equal to the given price with pagination.
     *
     * @param price    the maximum price
     * @param pageable pagination information
     * @return page of products with price less than or equal to the given price
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByMaxPrice(BigDecimal price, Pageable pageable) {
        return productRepository.findByPriceLessThanEqual(price, pageable);
    }

    /**
     * Get products by price less than or equal to the given price without pagination (legacy method).
     *
     * @param price the maximum price
     * @return list of products with price less than or equal to the given price
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByMaxPrice(BigDecimal price) {
        return productRepository.findByPriceLessThanEqual(price);
    }

    /**
     * Get products containing the given name (case-insensitive) with pagination.
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return page of products containing the given name
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Get products containing the given name (case-insensitive) without pagination (legacy method).
     *
     * @param name the name to search for
     * @return list of products containing the given name
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Create a new product.
     *
     * @param product the product to create
     * @return the created product
     * @throws EntityNotFoundException if the category does not exist
     */
    public Product createProduct(Product product) {
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepository.findById(product.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + product.getCategory().getId()));
            product.setCategory(category);
        }
        return productRepository.save(product);
    }

    /**
     * Update an existing product.
     *
     * @param id             the product ID
     * @param productDetails the updated product details
     * @return the updated product
     * @throws EntityNotFoundException if product not found or category not found
     */
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setImageUrl(productDetails.getImageUrl());

        if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
            Category category = categoryRepository.findById(productDetails.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + productDetails.getCategory().getId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return productRepository.save(product);
    }

    /**
     * Delete a product by ID.
     *
     * @param id the product ID
     * @throws EntityNotFoundException if product not found
     */
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
