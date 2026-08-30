package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.PublicationMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublicationMediaRepository extends JpaRepository<PublicationMediaEntity, UUID> {
}
