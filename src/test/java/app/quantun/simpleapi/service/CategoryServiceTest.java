package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.Category;
import app.quantun.simpleapi.repository.CategoryRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the CategoryService.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category1;
    private Category category2;
    private List<Category> categories;
    private Page<Category> categoryPage;

    @BeforeEach
    void setUp() {
        // Create test categories
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Electronics");
        category1.setDescription("Electronic devices");

        category2 = new Category();
        category2.setId(2L);
        category2.setName("Books");
        category2.setDescription("Reading materials");

        categories = Arrays.asList(category1, category2);
        categoryPage = new PageImpl<>(categories);
    }

    @Test
    @DisplayName("Should get all categories with pagination")
    void shouldGetAllCategoriesWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

        // Act
        Page<Category> result = categoryService.getAllCategories(pageable);

        // Assert
        assertThat(result).isEqualTo(categoryPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Electronics");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Books");
        verify(categoryRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should get all categories without pagination")
    void shouldGetAllCategoriesWithoutPagination() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertThat(result).isEqualTo(categories);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
        assertThat(result.get(1).getName()).isEqualTo("Books");
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get category by ID")
    void shouldGetCategoryById() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));

        // Act
        Category result = categoryService.getCategoryById(1L);

        // Assert
        assertThat(result).isEqualTo(category1);
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when category not found")
    void shouldThrowEntityNotFoundExceptionWhenCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.getCategoryById(999L);
        });
        verify(categoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should create category")
    void shouldCreateCategory() {
        // Arrange
        Category newCategory = new Category();
        newCategory.setName("New Category");
        newCategory.setDescription("New Description");

        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

        // Act
        Category result = categoryService.createCategory(newCategory);

        // Assert
        assertThat(result).isEqualTo(newCategory);
        assertThat(result.getName()).isEqualTo("New Category");
        verify(categoryRepository, times(1)).save(newCategory);
    }

    @Test
    @DisplayName("Should update category")
    void shouldUpdateCategory() {
        // Arrange
        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Category result = categoryService.updateCategory(1L, updatedCategory);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated Category");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent category")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentCategory() {
        // Arrange
        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.updateCategory(999L, updatedCategory);
        });
        verify(categoryRepository, times(1)).findById(999L);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should delete category")
    void shouldDeleteCategory() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        doNothing().when(categoryRepository).delete(any(Category.class));

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).delete(category1);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent category")
    void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentCategory() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.deleteCategory(999L);
        });
        verify(categoryRepository, times(1)).findById(999L);
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}