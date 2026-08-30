package alberto.cruz.tiendauniapi.utils.mapper;

import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationMediaEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationStatus;
import alberto.cruz.tiendauniapi.persistence.entity.TagEntity;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.presentation.dto.MediaContentRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PublicationMapper {

    public static PostSummaryResponse toPostSummary(PublicationEntity entity) {
        List<MediaContentRequest> mediaContent = toMediaContent(entity.getMedia());

        return createPostSummary(entity, mediaContent);
    }

    public static List<PostSummaryResponse> toPostSummary(Collection<PublicationEntity> entities) {
        return entities.stream()
                .map(PublicationMapper::toPostSummary)
                .toList();
    }

    public static PostDetailResponse toPostDetail(PublicationEntity publication, List<MediaContentRequest> mediaContent, List<ProductItem> products) {
        boolean isPublished = publication.getStatus() == PublicationStatus.PUBLISHED;

        return new PostDetailResponse(
                publication.getId(),
                publication.getTitle(),
                publication.getDescription(),
                mediaContent,
                products,
                publication.getPostedAt(),
                publication.getExpiredAt(),
                isPublished
        );
    }

    public static MediaContentRequest toMediaContent(PublicationMediaEntity entity) {
        return new MediaContentRequest(entity.getMediaType(), entity.getMediaUrl(), entity.getDisplayOrder());
    }

    public static List<MediaContentRequest> toMediaContent(Collection<PublicationMediaEntity> entities) {
        return entities.stream()
                .map(PublicationMapper::toMediaContent)
                .toList();
    }

    public static PublicationMediaEntity toPublicationMedia(MediaContentRequest media, PublicationEntity publication) {
        return PublicationMediaEntity.builder()
                .mediaType(media.mediaType())
                .mediaUrl(media.mediaKey())
                .displayOrder(media.orderNumber())
                .publication(publication)
                .build();
    }

    public static PublicationEntity toPublication(PostRequest request, UserEntity user, List<ProductEntity> products, List<TagEntity> tags) {
        var publishRightNow = request.publishRightNow() != null && request.publishRightNow();
        Instant postedAt = publishRightNow ? Instant.now() : null;

        var publicationStatus = publishRightNow ? PublicationStatus.PUBLISHED : PublicationStatus.DRAFT;

        Set<ProductEntity> productSet = new HashSet<>(products);
        Set<TagEntity> tagSet = new HashSet<>(tags);

        return PublicationEntity.builder()
                .title(request.title())
                .description(request.description())
                .expiredAt(request.expirationDate())
                .postedAt(postedAt)
                .status(publicationStatus)
                .user(user)
                .products(productSet)
                .tags(tagSet)
                .build();
    }

    private static PostSummaryResponse createPostSummary(PublicationEntity entity, List<MediaContentRequest> mediaContent) {
        return new PostSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                mediaContent,
                entity.getPostedAt(),
                entity.getStatus().name()
        );
    }

}
