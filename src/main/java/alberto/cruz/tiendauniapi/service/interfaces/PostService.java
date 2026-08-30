package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequestParams;
import alberto.cruz.tiendauniapi.presentation.dto.PostSummaryResponse;
import alberto.cruz.tiendauniapi.service.model.PostId;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface PostService {

    UUID createPost(PostRequest request, UUID userId);

    void publishPost(PostId id, UUID userId, Instant expirationDate);

    DataPaginationResponse<PostSummaryResponse> getPostsWithPagination(PostRequestParams requestParams, UUID universityId);

    DataPaginationResponse<PostSummaryResponse> getPostsByUser(UUID userId, String cursor, Pageable pageable);

    PostDetailResponse getPostById(PostId id, UUID universityId);

    void deletePost(PostId id, UUID userId);

    void updatePost(PostId id, UUID userId, PostRequest request);

    void changePostStatus(PostId id, UUID userId, String status);

}
