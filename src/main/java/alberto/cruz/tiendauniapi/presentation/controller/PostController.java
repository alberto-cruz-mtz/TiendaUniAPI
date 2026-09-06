package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.PostRequestParams;
import alberto.cruz.tiendauniapi.presentation.dto.PostSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PublishPostRequest;
import alberto.cruz.tiendauniapi.presentation.dto.StatusPostRequest;
import alberto.cruz.tiendauniapi.service.interfaces.PostService;
import alberto.cruz.tiendauniapi.service.model.PostId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<DataPaginationResponse<PostSummaryResponse>> getAllPosts(
            @PageableDefault(size = 15, direction = Sort.Direction.DESC, sort = {"postedAt", "id"}) Pageable pageRequest,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(required = false, name = "cursor") String cursor,
            @RequestParam(required = false, defaultValue = "false") Boolean outOfStock,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        var params = new PostRequestParams(cursor, pageRequest, search, outOfStock);
        var universityId = authenticatedUser.getUniversityId();

        var posts = postService.getPostsWithPagination(params, universityId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/me")
    public ResponseEntity<DataPaginationResponse<PostSummaryResponse>> getPostsByUser(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC, sort = {"postedAt", "id"}) Pageable pageRequest,
            @RequestParam(required = false, name = "cursor") String cursor,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        var userId = authenticatedUser.getUserId();
        var posts = postService.getPostsByUser(userId, cursor, pageRequest);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPostById(@PathVariable("id") String id, @RequestParam(required = false) String cursor, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        PostId postId = new PostId(id);
        UUID universityId = authenticatedUser.getUniversityId();
        PostDetailResponse post = postService.getPostById(postId, universityId);

        return ResponseEntity.ok(post);
    }


    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody @Valid PostRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UUID userId = authenticatedUser.getUserId();
        UUID postId = postService.createPost(request, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("{id}")
                .buildAndExpand(postId)
                .toUri();

        return ResponseEntity.created(location).body(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editPost(@PathVariable("id") String id, @RequestBody @Valid PostRequest post, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        PostId postId = new PostId(id);
        UUID userId = authenticatedUser.getUserId();
        postService.updatePost(postId, userId, post);

        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable("id") String id, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        PostId postId = new PostId(id);
        postService.deletePost(postId, authenticatedUser.getUserId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<?> publishPost(@PathVariable("id") String id, @RequestBody @Valid PublishPostRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        PostId postId = new PostId(id);
        postService.publishPost(postId, authenticatedUser.getUserId(), request.expirationDate());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> changePostStatus(@PathVariable("id") String id, @RequestBody @Valid StatusPostRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        PostId postId = new PostId(id);
        postService.changePostStatus(postId, authenticatedUser.getUserId(), request.status());

        return ResponseEntity.noContent().build();
    }
}
