package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByClientKey(UUID clientKey);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    List<OrderEntity> findAllByUserId(UUID userId);
}
