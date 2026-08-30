package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PublicationRepository extends JpaRepository<PublicationEntity, UUID>, JpaSpecificationExecutor<PublicationEntity> {

    @Override
    @EntityGraph(attributePaths = {"media"})
    @NonNull Page<PublicationEntity> findAll(@NonNull Specification<PublicationEntity> spec, @NonNull Pageable pageable);

    Optional<PublicationEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
