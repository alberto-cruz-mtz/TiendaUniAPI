package alberto.cruz.tiendauniapi.persistence.entity;

import alberto.cruz.tiendauniapi.common.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "universities")
public class UniversityEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "acronym", nullable = false, length = 15)
    private String acronym;

    @Column(name = "logo_url", nullable = false, length = 300)
    private String logoUrl;

    @Column(name = "brand_color", nullable = false, length = 30)
    private String brandColor;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "email_domains",
            joinColumns = @JoinColumn(name = "university_id")
    )
    @Column(name = "domain", nullable = false, length = 50)
    private Set<String> emailDomains = new HashSet<>();
}
