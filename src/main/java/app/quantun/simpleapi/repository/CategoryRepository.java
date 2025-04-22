package app.quantun.simpleapi.repository;

import app.quantun.simpleapi.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Category entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its name.
     *
     * @param name the name of the category
     * @return the category with the given name, or null if not found
     */
    Category findByName(String name);
}