package alberto.cruz.tiendauniapi.persistence.projection;

import java.util.UUID;

public interface UserProjection {

    UUID getId();

    String getEmail();

    String getPassword();

    UUID getUniversityId();
}
