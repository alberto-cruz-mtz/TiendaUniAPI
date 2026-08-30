package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.common.ResourceNotFoundException;
import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationMediaEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationStatus;
import alberto.cruz.tiendauniapi.persistence.entity.TagEntity;
import alberto.cruz.tiendauniapi.persistence.entity.TagName;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.ProductRepository;
import alberto.cruz.tiendauniapi.persistence.repository.PublicationMediaRepository;
import alberto.cruz.tiendauniapi.persistence.repository.PublicationRepository;
import alberto.cruz.tiendauniapi.persistence.repository.TagRepository;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.persistence.specification.PublicationSpecification;
import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.MediaContentRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequestParams;
import alberto.cruz.tiendauniapi.presentation.dto.PostSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.service.helper.CursorUtils;
import alberto.cruz.tiendauniapi.service.interfaces.PostService;
import alberto.cruz.tiendauniapi.service.model.Cursor;
import alberto.cruz.tiendauniapi.service.model.PostId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PublicationRepository publicationRepository;
    private final PublicationMediaRepository publicationMediaRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public UUID createPost(PostRequest request, UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));

        List<UUID> productIds;

        try {
            productIds = request.products().stream().map(UUID::fromString).toList();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Uno o más datos del campo products no es un ID valido");
        }

        List<ProductEntity> products = productRepository.findAllByIdIn(productIds);

        var tagNames = request.tags().stream().map(TagName::valueOf).toList();
        List<TagEntity> tags = tagRepository.findAllByNameIn(tagNames);

        var publishRightNow = request.publishRightNow() != null && request.publishRightNow();
        var status = publishRightNow ? PublicationStatus.PUBLISHED : PublicationStatus.DRAFT;

        Instant expirationDate = request.expirationDate();
        Instant postedAt = publishRightNow ? Instant.now() : null;

        PublicationEntity publication = PublicationEntity.builder()
                .title(request.title())
                .description(request.description())
                .expiredAt(expirationDate)
                .postedAt(postedAt)
                .status(status)
                .user(user)
                .products(new HashSet<>(products))
                .tags(new HashSet<>(tags))
                .build();

        var savedPublication = publicationRepository.save(publication);

        var publicationMediaEntities = request.mediaContent().stream()
                .map(media -> {
                    return PublicationMediaEntity.builder()
                            .mediaType(media.mediaType())
                            .mediaUrl(media.mediaKey())
                            .displayOrder(media.orderNumber())
                            .publication(savedPublication)
                            .build();
                })
                .toList();

        publicationMediaRepository.saveAll(publicationMediaEntities);

        return savedPublication.getId();
    }

    @Override
    @Transactional
    public void publishPost(PostId id, UUID userId, Instant expirationDate) {
        PublicationEntity publication = publicationRepository.findByIdAndUserId(id.value(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada con ID: " + id.value()));

        if (publication.getPostedAt() == null) {
            publication.setPostedAt(Instant.now());
            publication.setExpiredAt(expirationDate);
            publicationRepository.save(publication);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<PostSummaryResponse> getPostsWithPagination(PostRequestParams requestParams, UUID universityId) {
        Cursor cursor = CursorUtils.decodeCursor(requestParams.cursor());

        Specification<PublicationEntity> specification = Specification
                .where(PublicationSpecification.withUniversityId(universityId))
                .and(PublicationSpecification.withoutPublished())
                .and(PublicationSpecification.withSearch(requestParams.search()))
                .and(PublicationSpecification.withOutOfStock(requestParams.isOutOfStock()))
                .and(PublicationSpecification.withCursor(cursor.postId(), cursor.postedAt()));

        Page<PublicationEntity> publications = publicationRepository.findAll(specification, requestParams.pageable());
        List<PostSummaryResponse> posts = publications.getContent().stream()
                .map(publication -> {
                    List<MediaContentRequest> mediaContent = publication.getMedia().stream()
                            .map(media -> new MediaContentRequest(media.getMediaType(), media.getMediaUrl(), media.getDisplayOrder()))
                            .toList();

                    return new PostSummaryResponse(
                            publication.getId(),
                            publication.getTitle(),
                            publication.getDescription(),
                            mediaContent,
                            publication.getPostedAt(),
                            publication.getStatus().name()
                    );
                })
                .toList();

        boolean hasNext = publications.getTotalElements() > requestParams.pageable().getPageSize();

        String encodeCursor = null;

        if (hasNext) {
            PostSummaryResponse lastPost = posts.getLast();
            encodeCursor = CursorUtils.encodeCursor(lastPost.id(), lastPost.postedAt());
        }

        return new DataPaginationResponse<>(posts, encodeCursor, hasNext);
    }

    @Override
    public DataPaginationResponse<PostSummaryResponse> getPostsByUser(UUID userId, String cursor, Pageable pageable) {
        Cursor decodeCursor = CursorUtils.decodeCursor(cursor);

        Specification<PublicationEntity> specification = Specification
                .where(PublicationSpecification.withUserId(userId))
                .and(PublicationSpecification.withCursor(decodeCursor.postId(), decodeCursor.postedAt()));

        Page<PublicationEntity> publications = publicationRepository.findAll(specification, pageable);

        List<PostSummaryResponse> posts = publications.getContent().stream()
                .map(publication -> {
                    List<MediaContentRequest> mediaContent = publication.getMedia().stream()
                            .map(media -> new MediaContentRequest(media.getMediaType(), media.getMediaUrl(), media.getDisplayOrder()))
                            .toList();

                    return new PostSummaryResponse(
                            publication.getId(),
                            publication.getTitle(),
                            publication.getDescription(),
                            mediaContent,
                            publication.getPostedAt(),
                            publication.getStatus().name()
                    );
                })
                .toList();

        boolean hasNext = publications.getTotalElements() > pageable.getPageSize();
        log.info("Number of elements: {}, Page size: {}, Has next: {}", publications.getTotalElements(), pageable.getPageSize(), hasNext);

        String encodeCursor = null;

        if (hasNext) {
            PostSummaryResponse lastPost = posts.getLast();
            encodeCursor = CursorUtils.encodeCursor(lastPost.id(), lastPost.postedAt());
        }

        return new DataPaginationResponse<>(posts, encodeCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPostById(PostId id, UUID universityId) {
        PublicationEntity publication = publicationRepository.findById(id.value())
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada con ID: " + id.value()));

        List<ProductItem> products = publication.getProducts().stream()
                .map(product -> new ProductItem(product.getId(), product.getName(), product.getQuantity(), product.getSalePrice(), product.getCategory().getName().name(), product.getPhotoUrl()))
                .toList();

        List<MediaContentRequest> mediaContent = publication.getMedia().stream()
                .map(media -> new MediaContentRequest(media.getMediaType(), media.getMediaUrl(), media.getDisplayOrder()))
                .toList();

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

    @Override
    @Transactional
    public void deletePost(PostId id, UUID userId) {
        if (publicationRepository.existsByIdAndUserId(id.value(), userId)) {
            publicationRepository.deleteById(id.value());
            return;
        }

        throw new ResourceNotFoundException("Publication no encontrada con ID: " + id.value());
    }

    @Override
    @Transactional
    public void updatePost(PostId id, UUID userId, PostRequest request) {
        PublicationEntity publication = publicationRepository.findByIdAndUserId(id.value(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro ninguna publicacion con el ID proporcionado"));

        List<UUID> productIds;

        try {
            productIds = request.products().stream().map(UUID::fromString).toList();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Uno o más datos del campo products no es un ID valido");
        }

        List<ProductEntity> products = productRepository.findAllByIdIn(productIds);
        publication.getProducts().clear();
        publication.getProducts().addAll(products);

        var tagNames = request.tags().stream().map(TagName::valueOf).toList();
        List<TagEntity> tags = tagRepository.findAllByNameIn(tagNames);
        if (!tags.isEmpty()) {
            publication.getTags().clear();
            publication.getTags().addAll(tags);
        }

        publication.setTitle(request.title());
        publication.setDescription(request.description());
        publication.setExpiredAt(request.expirationDate());

        var publicationMediaEntities = request.mediaContent().stream()
                .map(media ->
                        PublicationMediaEntity.builder()
                                .mediaType(media.mediaType())
                                .mediaUrl(media.mediaKey())
                                .displayOrder(media.orderNumber())
                                .publication(publication)
                                .build()
                )
                .toList();

        publication.getMedia().clear();
        publication.getMedia().addAll(publicationMediaEntities);

        publicationRepository.save(publication);
    }

    @Override
    public void changePostStatus(PostId id, UUID userId, String status) {
        PublicationEntity publication = publicationRepository.findByIdAndUserId(id.value(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada con ID: " + id.value()));

        PublicationStatus newStatus = PublicationStatus.valueOf(status.toUpperCase());

        if (newStatus == PublicationStatus.EXPIRED) {
            publication.setExpiredAt(Instant.now());
        }

        publication.setStatus(newStatus);
        publicationRepository.save(publication);
    }
}