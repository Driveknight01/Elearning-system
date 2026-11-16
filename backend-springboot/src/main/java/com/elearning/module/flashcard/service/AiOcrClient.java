package com.elearning.module.flashcard.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.config.AiServiceProperties;
import com.elearning.module.flashcard.dto.OcrExtractResponse;
import com.elearning.module.flashcard.dto.OcrExtractedCardDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client communicating with the Python FastAPI {@code ai-ml-service} OCR endpoint
 * ({@code POST /ocr/extract}) with configured timeout and standardized {@link ErrorCode#AI_SERVICE_UNAVAILABLE} fallback.
 */
@Slf4j
@Component
public class AiOcrClient {

    private final RestClient restClient;

    @Autowired
    public AiOcrClient(AiServiceProperties properties) {
        this(buildDefaultRestClient(properties));
    }

    /**
     * Package-private constructor for unit testing with {@code MockRestServiceServer}.
     */
    AiOcrClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient buildDefaultRestClient(AiServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1000, properties.getOcrTimeoutMs());
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        String baseUrl = properties.getBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return RestClient.builder()
                .baseUrl(baseUrl != null ? baseUrl : "http://localhost:8000")
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Sends an image file to {@code ai-ml-service /ocr/extract} and maps the parsed cards response.
     */
    public OcrExtractResponse extractCards(MultipartFile file, String sourceLang, String targetLang) {
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể đọc dữ liệu tệp ảnh tải lên");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "upload.jpg";
        MediaType partMediaType = resolvePartMediaType(file.getContentType());

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(partMediaType);
        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, fileHeaders));
        if (StringUtils.hasText(sourceLang)) {
            body.add("source_lang", sourceLang.trim());
        }
        if (StringUtils.hasText(targetLang)) {
            body.add("target_lang", targetLang.trim());
        }

        try {
            FastApiOcrResponse remote = restClient.post()
                    .uri("/ocr/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(FastApiOcrResponse.class);

            if (remote == null) {
                throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Dịch vụ OCR AI không trả về dữ liệu");
            }

            List<OcrExtractedCardDto> cards = remote.getCards() == null
                    ? Collections.emptyList()
                    : remote.getCards().stream()
                    .map(c -> OcrExtractedCardDto.builder()
                            .term(c.getTerm())
                            .definition(c.getDefinition())
                            .build())
                    .toList();

            return OcrExtractResponse.builder()
                    .rawText(remote.getRawText() != null ? remote.getRawText() : "")
                    .cards(cards)
                    .confidence(remote.getConfidence() != null ? remote.getConfidence() : 0.0)
                    .engine(remote.getEngine() != null ? remote.getEngine() : "unknown")
                    .warnings(remote.getWarnings() != null ? remote.getWarnings() : Collections.emptyList())
                    .build();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Tệp ảnh không hợp lệ để trích xuất OCR");
            }
            if (ex.getStatusCode().value() == HttpStatus.PAYLOAD_TOO_LARGE.value()) {
                throw new AppException(ErrorCode.FILE_TOO_LARGE, "Dung lượng ảnh vượt quá giới hạn cho phép");
            }
            log.warn("AI OCR service returned error status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "Dịch vụ OCR AI tạm thời không khả dụng, vui lòng thử lại sau"
            );
        } catch (RestClientException ex) {
            log.warn("Failed to reach AI OCR service: {}", ex.getMessage());
            throw new AppException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "Dịch vụ OCR AI tạm thời không khả dụng, vui lòng thử lại sau"
            );
        }
    }

    private MediaType resolvePartMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.IMAGE_JPEG;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.IMAGE_JPEG;
        }
    }

    @Data
    private static class FastApiOcrResponse {
        private boolean success;

        @JsonProperty("raw_text")
        private String rawText;

        private List<FastApiOcrCard> cards;
        private Double confidence;
        private String engine;
        private List<String> warnings;
    }

    @Data
    private static class FastApiOcrCard {
        private String term;
        private String definition;
    }
}
