package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.UniversityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UniversityRepository extends JpaRepository<UniversityEntity, UUID> {

    Optional<UniversityEntity> findByAcronym(String acronym);

    boolean existsByAcronym(String acronym);

    Optional<UniversityEntity> findByEmailDomainsContains(String domain);
}
