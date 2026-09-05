package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.OrderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByClientKey(UUID clientKey);

    @EntityGraph(attributePaths = {"productOrders", "productOrders.product"})
    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    Slice<OrderEntity> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.publication.id = :publicationId AND o.publication.user.id = :userId")
    Slice<OrderEntity> findAllByPublicationIdAndUserId(@Param("publicationId") UUID publicationId, @Param("userId") UUID userId, Pageable pageable);
}
