package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PublicationRepository extends JpaRepository<PublicationEntity, UUID>, JpaSpecificationExecutor<PublicationEntity> {

    @Override
    @EntityGraph(attributePaths = {"media"})
    @NonNull Page<PublicationEntity> findAll(@NonNull Specification<PublicationEntity> spec, @NonNull Pageable pageable);

    Optional<PublicationEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("select p from PublicationEntity p where p.id = :id and p.user.university.id = :universityId")
    Optional<PublicationEntity> findByIdAndUniversityId(@Param("id") UUID id, @Param("universityId") UUID universityId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
