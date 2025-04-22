package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Category operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get all categories with pagination.
     *
     * @param pageable pagination information
     * @return page of categories
     */
    @Transactional(readOnly = true)
    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    /**
     * Get all categories without pagination (legacy method).
     *
     * @return list of all categories
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get category by ID.
     *
     * @param id the category ID
     * @return the category with the given ID
     * @throws EntityNotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    /**
     * Create a new category.
     *
     * @param category the category to create
     * @return the created category
     */
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    /**
     * Update an existing category.
     *
     * @param id the category ID
     * @param categoryDetails the updated category details
     * @return the updated category
     * @throws EntityNotFoundException if category not found
     */
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());

        return categoryRepository.save(category);
    }

    /**
     * Delete a category by ID.
     *
     * @param id the category ID
     * @throws EntityNotFoundException if category not found
     */
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
