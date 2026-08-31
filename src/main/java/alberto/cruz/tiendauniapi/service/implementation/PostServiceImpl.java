package alberto.cruz.tiendauniapi.service.implementation;

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
import alberto.cruz.tiendauniapi.persistence.specification.PublicationSpecification;
import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.MediaContentRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequestParams;
import alberto.cruz.tiendauniapi.presentation.dto.PostSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.service.exception.InvalidArgumentException;
import alberto.cruz.tiendauniapi.service.exception.PostAlreadyPublishedException;
import alberto.cruz.tiendauniapi.service.exception.PostNotFoundException;
import alberto.cruz.tiendauniapi.service.helper.CursorUtils;
import alberto.cruz.tiendauniapi.service.interfaces.PostService;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
import alberto.cruz.tiendauniapi.service.model.Cursor;
import alberto.cruz.tiendauniapi.service.model.PostId;
import alberto.cruz.tiendauniapi.utils.mapper.ProductMapper;
import alberto.cruz.tiendauniapi.utils.mapper.PublicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PublicationRepository publicationRepository;
    private final PublicationMediaRepository publicationMediaRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public UUID createPost(PostRequest request, UUID userId) {
        PublicationEntity publication = this.buildPublication(request, userId);
        PublicationEntity savedPublication = publicationRepository.save(publication);

        List<PublicationMediaEntity> publicationMediaEntities = this.createPublicationMediaList(publication, request.mediaContent());
        publicationMediaRepository.saveAll(publicationMediaEntities);

        return savedPublication.getId();
    }

    @Override
    @Transactional
    public void publishPost(PostId id, UUID userId, Instant expirationDate) {
        PublicationEntity publication = this.findOwnedPublication(id.value(), userId);

        boolean isAlreadyPublished = publication.getStatus() == PublicationStatus.PUBLISHED;
        if (isAlreadyPublished) throw new PostAlreadyPublishedException();

        publication.setPostedAt(Instant.now());
        publication.setExpiredAt(expirationDate);
        publication.setStatus(PublicationStatus.PUBLISHED);
        publicationRepository.save(publication);
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<PostSummaryResponse> getPostsWithPagination(PostRequestParams requestParams, UUID universityId) {
        Page<PublicationEntity> publications = this.searchPublications(requestParams, universityId);
        return this.toDataPaginationResponse(publications, requestParams.pageable());
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<PostSummaryResponse> getPostsByUser(UUID userId, String cursor, Pageable pageable) {
        Page<PublicationEntity> publications = this.findPublicationsByUser(userId, cursor, pageable);
        return this.toDataPaginationResponse(publications, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPostById(PostId id, UUID universityId) {
        PublicationEntity publication = this.findPublicationById(id.value(), universityId);

        List<ProductItem> products = ProductMapper.toProductItem(publication.getProducts());
        List<MediaContentRequest> mediaContent = PublicationMapper.toMediaContent(publication.getMedia());

        return PublicationMapper.toPostDetail(publication, mediaContent, products);
    }

    @Override
    @Transactional
    public void deletePost(PostId id, UUID userId) {
        if (publicationRepository.existsByIdAndUserId(id.value(), userId)) {
            publicationRepository.deleteById(id.value());
            return;
        }

        throw new PostNotFoundException();
    }

    @Override
    @Transactional
    public void updatePost(PostId id, UUID userId, PostRequest request) {
        PublicationEntity publication = this.findOwnedPublication(id.value(), userId);

        this.replaceProductsInPublication(publication, request.products());
        this.replaceTagsInPublication(publication, request.tags());
        this.replaceMediaContentInPublication(publication, request.mediaContent());

        publication.setTitle(request.title());
        publication.setDescription(request.description());
        publication.setExpiredAt(request.expirationDate());

        publicationRepository.save(publication);
    }

    @Override
    public void changePostStatus(PostId id, UUID userId, String status) {
        PublicationEntity publication = this.findOwnedPublication(id.value(), userId);
        PublicationStatus newStatus = PublicationStatus.valueOf(status.toUpperCase());

        boolean isChangingToExpired = newStatus == PublicationStatus.EXPIRED;
        if (isChangingToExpired) {
            publication.setExpiredAt(Instant.now());
        }

        publication.setStatus(newStatus);
        publicationRepository.save(publication);
    }

    private Page<PublicationEntity> searchPublications(PostRequestParams params, UUID universityId) {
        Cursor cursor = CursorUtils.decodeCursor(params.cursor());

        Specification<PublicationEntity> specification = Specification
                .where(PublicationSpecification.withUniversityId(universityId))
                .and(PublicationSpecification.withoutPublished())
                .and(PublicationSpecification.withSearch(params.search()))
                .and(PublicationSpecification.withOutOfStock(params.isOutOfStock()))
                .and(PublicationSpecification.withCursor(cursor.postId(), cursor.postedAt()));

        return publicationRepository.findAll(specification, params.pageable());
    }

    private Page<PublicationEntity> findPublicationsByUser(UUID userId, String cursor, Pageable pageable) {
        Cursor decodedCursor = CursorUtils.decodeCursor(cursor);

        Specification<PublicationEntity> specification = Specification
                .where(PublicationSpecification.withUserId(userId))
                .and(PublicationSpecification.withCursor(decodedCursor.postId(), decodedCursor.postedAt()));

        return publicationRepository.findAll(specification, pageable);
    }

    private String generateNextCursor(List<PostSummaryResponse> posts, boolean hasNext) {
        String encodedCursor = null;

        if (hasNext) {
            PostSummaryResponse lastPost = posts.getLast();
            encodedCursor = CursorUtils.encodeCursor(lastPost.id(), lastPost.postedAt());
        }

        return encodedCursor;
    }

    private PublicationEntity findPublicationById(UUID id, UUID universityId) {
        return publicationRepository.findByIdAndUniversityId(id, universityId)
                .orElseThrow(PostNotFoundException::new);
    }

    private PublicationEntity findOwnedPublication(UUID id, UUID userId) {
        return publicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(PostNotFoundException::new);
    }

    private List<ProductEntity> findProductsByIds(List<String> productIds) {
        List<UUID> productUids;

        try {
            productUids = productIds.stream().map(UUID::fromString).toList();
        } catch (Exception exception) {
            throw new InvalidArgumentException("products", "Uno o más IDs de productos no es un ID de tipo UUID valido");
        }

        return productRepository.findAllByIdIn(productUids);
    }

    private void replaceProductsInPublication(PublicationEntity publication, List<String> productIds) {
        List<ProductEntity> products = this.findProductsByIds(productIds);

        if (!products.isEmpty()) {
            publication.getProducts().clear();
            publication.getProducts().addAll(products);
        }
    }

    private List<TagEntity> findTagsByNames(List<String> tagNames) {
        var tags = tagNames.stream().map(TagName::valueOf).toList();
        return tagRepository.findAllByNameIn(tags);
    }

    private void replaceTagsInPublication(PublicationEntity publication, List<String> tagNames) {
        var tags = this.findTagsByNames(tagNames);

        if (!tags.isEmpty()) {
            publication.getTags().clear();
            publication.getTags().addAll(tags);
        }
    }

    private List<PublicationMediaEntity> createPublicationMediaList(PublicationEntity publication, List<MediaContentRequest> mediaContentRequest) {
        return mediaContentRequest.stream()
                .map(media -> PublicationMapper.toPublicationMedia(media, publication))
                .toList();

    }

    private void replaceMediaContentInPublication(PublicationEntity publication, List<MediaContentRequest> mediaContentRequest) {
        var publicationMediaEntities = this.createPublicationMediaList(publication, mediaContentRequest);
        publication.getMedia().clear();
        publication.getMedia().addAll(publicationMediaEntities);
    }

    private PublicationEntity buildPublication(PostRequest request, UUID userId) {
        UserEntity user = userService.getUserById(userId);
        List<ProductEntity> products = this.findProductsByIds(request.products());
        List<TagEntity> tags = this.findTagsByNames(request.tags());

        return PublicationMapper.toPublication(request, user, products, tags);
    }

    private DataPaginationResponse<PostSummaryResponse> toDataPaginationResponse(Page<PublicationEntity> publications, Pageable pageable) {
        List<PostSummaryResponse> posts = PublicationMapper.toPostSummary(publications.getContent());

        boolean hasNext = publications.hasNext();
        String cursor = this.generateNextCursor(posts, hasNext);

        return new DataPaginationResponse<>(posts, cursor, hasNext);
    }
}