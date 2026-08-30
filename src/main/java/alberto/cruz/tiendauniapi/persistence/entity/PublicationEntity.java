package alberto.cruz.tiendauniapi.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "publications")
public class PublicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", nullable = false, length = 350)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublicationStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToMany(targetEntity = ProductEntity.class, fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_publication",
            joinColumns = @JoinColumn(name = "publication_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "product_id", nullable = false)
    )
    private Set<ProductEntity> products = new HashSet<>();

    @ManyToMany(targetEntity = TagEntity.class, fetch = FetchType.LAZY)
    @JoinTable(
            name = "tag_publication",
            joinColumns = @JoinColumn(name = "publication_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "tag_id", nullable = false)
    )
    @Builder.Default
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(mappedBy = "publication", fetch = FetchType.LAZY, targetEntity = PublicationMediaEntity.class, orphanRemoval = true, cascade = CascadeType.ALL)
    private List<PublicationMediaEntity> media = new ArrayList<>();
}
