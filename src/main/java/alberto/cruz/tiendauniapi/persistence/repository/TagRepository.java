package alberto.cruz.tiendauniapi.persistence.repository;

import alberto.cruz.tiendauniapi.persistence.entity.TagEntity;
import alberto.cruz.tiendauniapi.persistence.entity.TagName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<TagEntity, Integer> {

    List<TagEntity> findAllByNameIn(Collection<TagName> name);
}