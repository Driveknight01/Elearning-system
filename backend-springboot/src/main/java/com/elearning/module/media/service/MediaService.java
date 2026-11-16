package com.elearning.module.media.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.service.LocalStorageService;
import com.elearning.config.MediaProperties;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.repository.FlashcardRepository;
import com.elearning.module.media.dto.AiImageGenerateRequest;
import com.elearning.module.media.dto.AiImageGenerateResponse;
import com.elearning.module.media.dto.MediaUploadResponse;
import com.elearning.module.media.dto.StockImageItemDto;
import com.elearning.module.media.dto.StockImageSearchResponse;
import com.elearning.module.user.entity.Role;
import com.elearning.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service handling flashcard image uploads, free stock illustration search (Pixabay / Unsplash / fallback),
 * and AI mnemonic image generation (Pollinations) with optional persistence to {@code flashcards.image_url}.
 */
@Slf4j
@Service
public class MediaService {

    private final MediaProperties mediaProperties;
    private final LocalStorageService localStorageService;
    private final FlashcardRepository flashcardRepository;
    private final RestClient restClient;

    @Autowired
    public MediaService(
            MediaProperties mediaProperties,
            LocalStorageService localStorageService,
            FlashcardRepository flashcardRepository
    ) {
        this(mediaProperties, localStorageService, flashcardRepository, buildDefaultRestClient(mediaProperties));
    }

    /**
     * Package-private constructor for unit testing with {@code MockRestServiceServer}.
     */
    MediaService(
            MediaProperties mediaProperties,
            LocalStorageService localStorageService,
            FlashcardRepository flashcardRepository,
            RestClient restClient
    ) {
        this.mediaProperties = mediaProperties;
        this.localStorageService = localStorageService;
        this.flashcardRepository = flashcardRepository;
        this.restClient = restClient;
    }

    private static RestClient buildDefaultRestClient(MediaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1000, properties.getTimeoutMs());
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Transactional
    public MediaUploadResponse uploadCardImage(MultipartFile file, Long cardId, UserPrincipal currentUser) {
        String storedUrl = localStorageService.storeImage(file, "cards");
        Long savedToCardId = null;

        if (cardId != null) {
            Flashcard card = getOwnedCardOrThrow(cardId, currentUser);
            card.setImageUrl(storedUrl);
            flashcardRepository.save(card);
            savedToCardId = card.getId();
        }

        return MediaUploadResponse.builder()
                .url(storedUrl)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .savedToCardId(savedToCardId)
                .build();
    }

    public StockImageSearchResponse searchStockImages(String query, int perPage) {
        if (!StringUtils.hasText(query)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Từ khóa tìm kiếm ảnh không được để trống");
        }
        String trimmedQuery = query.trim();
        int clampedCount = Math.min(Math.max(perPage, 1), 30);

        if (isConfiguredKey(mediaProperties.getPixabayApiKey())) {
            try {
                List<StockImageItemDto> items = fetchFromPixabay(trimmedQuery, clampedCount);
                return StockImageSearchResponse.builder()
                        .query(trimmedQuery)
                        .provider("pixabay")
                        .items(items)
                        .build();
            } catch (RestClientException ex) {
                log.warn("Pixabay search failed for '{}', trying fallback: {}", trimmedQuery, ex.getMessage());
            }
        }

        if (isConfiguredKey(mediaProperties.getUnsplashAccessKey())) {
            try {
                List<StockImageItemDto> items = fetchFromUnsplash(trimmedQuery, clampedCount);
                return StockImageSearchResponse.builder()
                        .query(trimmedQuery)
                        .provider("unsplash")
                        .items(items)
                        .build();
            } catch (RestClientException ex) {
                log.warn("Unsplash search failed for '{}', trying fallback: {}", trimmedQuery, ex.getMessage());
            }
        }

        return StockImageSearchResponse.builder()
                .query(trimmedQuery)
                .provider("fallback")
                .items(buildFallbackSuggestions(trimmedQuery, Math.min(clampedCount, 4)))
                .build();
    }

    @Transactional
    public AiImageGenerateResponse generateAiMnemonicImage(
            AiImageGenerateRequest request,
            UserPrincipal currentUser
    ) {
        String term = request.getTerm().trim();
        String style = StringUtils.hasText(request.getStyle()) ? request.getStyle().trim() : "flat educational illustration";

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Clean ").append(style)
                .append(" representing the vocabulary concept '").append(term).append("'");
        if (StringUtils.hasText(request.getDefinition())) {
            promptBuilder.append(" meaning '").append(request.getDefinition().trim()).append("'");
        }
        promptBuilder.append(", minimalist, high contrast, no text, no watermark");

        String prompt = promptBuilder.toString();
        String baseUrl = StringUtils.hasText(mediaProperties.getPollinationsBaseUrl())
                ? mediaProperties.getPollinationsBaseUrl().trim()
                : "https://image.pollinations.ai/prompt";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        int seed = ThreadLocalRandom.current().nextInt(1, 1_000_000);
        String encodedPrompt = UriUtils.encodePathSegment(prompt, StandardCharsets.UTF_8);
        String imageUrl = baseUrl + "/" + encodedPrompt + "?width=768&height=512&nologo=true&seed=" + seed;

        Long savedToCardId = null;
        if (request.getCardId() != null) {
            Flashcard card = getOwnedCardOrThrow(request.getCardId(), currentUser);
            card.setImageUrl(imageUrl);
            flashcardRepository.save(card);
            savedToCardId = card.getId();
        }

        return AiImageGenerateResponse.builder()
                .imageUrl(imageUrl)
                .prompt(prompt)
                .provider("pollinations")
                .savedToCardId(savedToCardId)
                .build();
    }

    private Flashcard getOwnedCardOrThrow(Long cardId, UserPrincipal currentUser) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ Flashcard"));

        if (card.getFlashcardSet() != null && card.getFlashcardSet().getDeletedAt() != null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bộ thẻ chứa thẻ này đã bị xóa");
        }

        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isOwner = currentUser != null
                && card.getFlashcardSet() != null
                && card.getFlashcardSet().getOwner() != null
                && card.getFlashcardSet().getOwner().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền cập nhật ảnh cho thẻ này");
        }
        return card;
    }

    private List<StockImageItemDto> fetchFromPixabay(String query, int perPage) {
        String uri = UriComponentsBuilder.fromHttpUrl("https://pixabay.com/api/")
                .queryParam("key", mediaProperties.getPixabayApiKey().trim())
                .queryParam("q", query)
                .queryParam("image_type", "all")
                .queryParam("safesearch", "true")
                .queryParam("per_page", perPage)
                .toUriString();

        JsonNode root = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        List<StockImageItemDto> items = new ArrayList<>();
        if (root != null && root.has("hits") && root.get("hits").isArray()) {
            for (JsonNode hit : root.get("hits")) {
                items.add(StockImageItemDto.builder()
                        .id(hit.path("id").asText())
                        .previewUrl(hit.path("previewURL").asText())
                        .imageUrl(hit.path("webformatURL").asText())
                        .width(hit.path("webformatWidth").asInt(0))
                        .height(hit.path("webformatHeight").asInt(0))
                        .provider("pixabay")
                        .attribution("Pixabay / " + hit.path("user").asText("Contributor"))
                        .build());
            }
        }
        return items;
    }

    private List<StockImageItemDto> fetchFromUnsplash(String query, int perPage) {
        String uri = UriComponentsBuilder.fromHttpUrl("https://api.unsplash.com/search/photos")
                .queryParam("query", query)
                .queryParam("per_page", perPage)
                .toUriString();

        JsonNode root = restClient.get()
                .uri(uri)
                .header("Authorization", "Client-ID " + mediaProperties.getUnsplashAccessKey().trim())
                .retrieve()
                .body(JsonNode.class);

        List<StockImageItemDto> items = new ArrayList<>();
        if (root != null && root.has("results") && root.get("results").isArray()) {
            for (JsonNode res : root.get("results")) {
                items.add(StockImageItemDto.builder()
                        .id(res.path("id").asText())
                        .previewUrl(res.path("urls").path("small").asText())
                        .imageUrl(res.path("urls").path("regular").asText())
                        .width(res.path("width").asInt(0))
                        .height(res.path("height").asInt(0))
                        .provider("unsplash")
                        .attribution("Unsplash / " + res.path("user").path("name").asText("Photographer"))
                        .build());
            }
        }
        return items;
    }

    private List<StockImageItemDto> buildFallbackSuggestions(String query, int count) {
        String baseUrl = StringUtils.hasText(mediaProperties.getPollinationsBaseUrl())
                ? mediaProperties.getPollinationsBaseUrl().trim()
                : "https://image.pollinations.ai/prompt";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String[] styles = {"flat vector icon", "minimalist illustration", "watercolor sketch", "3d isometric"};
        List<StockImageItemDto> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String style = styles[i % styles.length];
            String prompt = query + ", " + style + ", educational flashcard visual, no text";
            String url = baseUrl + "/" + UriUtils.encodePathSegment(prompt, StandardCharsets.UTF_8)
                    + "?width=512&height=384&nologo=true&seed=" + (100 + i);
            items.add(StockImageItemDto.builder()
                    .id("fallback-" + (i + 1))
                    .previewUrl(url)
                    .imageUrl(url)
                    .width(512)
                    .height(384)
                    .provider("fallback")
                    .attribution("AI Fallback Illustration (" + style + ")")
                    .build());
        }
        return items;
    }

    private boolean isConfiguredKey(String key) {
        return StringUtils.hasText(key)
                && !key.trim().equalsIgnoreCase("changeme")
                && !key.trim().startsWith("your_");
    }
}
