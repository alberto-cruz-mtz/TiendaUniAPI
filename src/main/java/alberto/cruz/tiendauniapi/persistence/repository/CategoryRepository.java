package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {
}