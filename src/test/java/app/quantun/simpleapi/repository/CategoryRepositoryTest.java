package app.quantun.simpleapi.repository;

import app.quantun.simpleapi.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the CategoryRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Should save a category")
    void shouldSaveCategory() {
        // Arrange
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");

        // Act
        Category savedCategory = categoryRepository.save(category);

        // Assert
        assertThat(savedCategory).isNotNull();
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Test Category");
        assertThat(savedCategory.getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("Should find a category by ID")
    void shouldFindCategoryById() {
        // Arrange
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");
        Category savedCategory = categoryRepository.save(category);

        // Act
        Optional<Category> foundCategory = categoryRepository.findById(savedCategory.getId());

        // Assert
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Test Category");
        assertThat(foundCategory.get().getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("Should find a category by name")
    void shouldFindCategoryByName() {
        // Arrange
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");
        categoryRepository.save(category);

        // Act
        Category foundCategory = categoryRepository.findByName("Test Category");

        // Assert
        assertThat(foundCategory).isNotNull();
        assertThat(foundCategory.getName()).isEqualTo("Test Category");
        assertThat(foundCategory.getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("Should return null when finding by non-existent name")
    void shouldReturnNullWhenFindingByNonExistentName() {
        // Act
        Category foundCategory = categoryRepository.findByName("Non-existent Category");

        // Assert
        assertThat(foundCategory).isNull();
    }

    @Test
    @DisplayName("Should find all categories")
    void shouldFindAllCategories() {
        // Arrange
        Category category1 = new Category();
        category1.setName("Category 1");
        category1.setDescription("Description 1");

        Category category2 = new Category();
        category2.setName("Category 2");
        category2.setDescription("Description 2");

        categoryRepository.saveAll(List.of(category1, category2));

        // Act
        List<Category> categories = categoryRepository.findAll();

        // Assert
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder("Category 1", "Category 2");
    }

    @Test
    @DisplayName("Should find all categories with pagination")
    void shouldFindAllCategoriesWithPagination() {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            Category category = new Category();
            category.setName("Category " + i);
            category.setDescription("Description " + i);
            categoryRepository.save(category);
        }

        // Act
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").ascending());
        Page<Category> categoryPage = categoryRepository.findAll(pageable);

        // Assert
        assertThat(categoryPage.getContent()).hasSize(5);
        assertThat(categoryPage.getTotalElements()).isEqualTo(10);
        assertThat(categoryPage.getTotalPages()).isEqualTo(2);
        assertThat(categoryPage.getContent()).extracting(Category::getName)
                .containsExactly("Category 1", "Category 10", "Category 2", "Category 3", "Category 4");
    }

    @Test
    @DisplayName("Should update a category")
    void shouldUpdateCategory() {
        // Arrange
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");
        Category savedCategory = categoryRepository.save(category);

        // Act
        savedCategory.setName("Updated Category");
        savedCategory.setDescription("Updated Description");
        Category updatedCategory = categoryRepository.save(savedCategory);

        // Assert
        assertThat(updatedCategory.getId()).isEqualTo(savedCategory.getId());
        assertThat(updatedCategory.getName()).isEqualTo("Updated Category");
        assertThat(updatedCategory.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("Should delete a category")
    void shouldDeleteCategory() {
        // Arrange
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");
        Category savedCategory = categoryRepository.save(category);

        // Act
        categoryRepository.delete(savedCategory);
        Optional<Category> foundCategory = categoryRepository.findById(savedCategory.getId());

        // Assert
        assertThat(foundCategory).isEmpty();
    }
}