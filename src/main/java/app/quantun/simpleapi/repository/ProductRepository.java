package app.quantun.simpleapi.repository;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find products by category.
     *
     * @param category the category
     * @return list of products in the given category
     */
    List<Product> findByCategory(Category category);

    /**
     * Find products by category with pagination.
     *
     * @param category the category
     * @param pageable pagination information
     * @return page of products in the given category
     */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /**
     * Find products by category ID.
     *
     * @param categoryId the category ID
     * @return list of products in the given category
     */
    List<Product> findByCategoryId(Long categoryId);

    /**
     * Find products by category ID with pagination.
     *
     * @param categoryId the category ID
     * @param pageable   pagination information
     * @return page of products in the given category
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Find products with price less than or equal to the given price.
     *
     * @param price the maximum price
     * @return list of products with price less than or equal to the given price
     */
    List<Product> findByPriceLessThanEqual(BigDecimal price);

    /**
     * Find products with price less than or equal to the given price with pagination.
     *
     * @param price    the maximum price
     * @param pageable pagination information
     * @return page of products with price less than or equal to the given price
     */
    Page<Product> findByPriceLessThanEqual(BigDecimal price, Pageable pageable);

    /**
     * Find products containing the given name (case-insensitive).
     *
     * @param name the name to search for
     * @return list of products containing the given name
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products containing the given name (case-insensitive) with pagination.
     *
     * @param name     the name to search for
     * @param pageable pagination information
     * @return page of products containing the given name
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
