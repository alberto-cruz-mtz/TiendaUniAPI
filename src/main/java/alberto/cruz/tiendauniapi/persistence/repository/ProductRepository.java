package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findAllByIdIn(Collection<UUID> ids);

    Optional<ProductEntity> findByIdAndUserId(UUID id, UUID userId);

    List<ProductEntity> findAllByUserId(@Param("userId") UUID userId);
}