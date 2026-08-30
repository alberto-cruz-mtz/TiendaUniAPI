package alberto.cruz.tiendauniapi.persistence.specification;

import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PublicationSpecification {

    public static Specification<PublicationEntity> withoutPublished() {
        return ((root, query, criteriaBuilder) -> {
            Predicate differentToNull = criteriaBuilder.isNotNull(root.get("postedAt"));
            Predicate notExpired = criteriaBuilder.greaterThan(root.get("expiredAt"), Instant.now());
            Predicate isPublished = criteriaBuilder.equal(root.get("status"), PublicationStatus.PUBLISHED);

            return criteriaBuilder.and(differentToNull, notExpired, isPublished);
        }); }

    public static Specification<PublicationEntity> withUniversityId(UUID universityId) {
        return ((root, query, criteriaBuilder) -> {
            // Se aplica la validacion de que el universityId no sea nulo antes de construir la especificacion
            // Dado que los usuarios solo pueden ver publicaciones de su propia universidad, no tiene sentido permitir un universityId nulo
            Objects.requireNonNull(universityId, "University ID must not be null");

            return criteriaBuilder.equal(root.get("user").get("university").get("id"), universityId);
        });
    }

    public static Specification<PublicationEntity> withCursor(UUID postId, Instant postedAt) {
        return ((root, query, criteriaBuilder) -> {
            if (postId == null || postedAt == null) return null;

            Predicate postedAtLess = criteriaBuilder.lessThan(root.get("postedAt"), postedAt);
            Predicate postedAtEqual = criteriaBuilder.equal(root.get("postedAt"), postedAt);
            Predicate idLess = criteriaBuilder.lessThan(root.get("id"), postId);

            Predicate tieBreaker = criteriaBuilder.and(postedAtEqual, idLess);
            return criteriaBuilder.or(postedAtLess, tieBreaker);
        });
    }

    public static Specification<PublicationEntity> withSearch(String searchTerm) {
        return ((root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.isEmpty()) return null;

            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
            );
        });
    }

    public static Specification<PublicationEntity> withOutOfStock(boolean isOutOfStock) {
        return ((root, query, criteriaBuilder) -> {
            if (!isOutOfStock) return null;

            return criteriaBuilder.isTrue(root.get("outOfStock"));
        });
    }

    public static Specification<PublicationEntity> withUserId(UUID userId) {
        return ((root, query, criteriaBuilder) -> {
            Objects.requireNonNull(userId, "User ID must not be null");

            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        });
    }
}
