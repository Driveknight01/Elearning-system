package com.elearning.module.flashcard.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.flashcard.dto.OcrExtractResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiOcrClientTest {

    private MockRestServiceServer mockServer;
    private AiOcrClient aiOcrClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        aiOcrClient = new AiOcrClient(builder.build());
    }

    @Test
    void extractCards_success_parsesFastApiResponse() {
        String fastApiJson = """
                {
                  "success": true,
                  "raw_text": "ephemeral - ngắn ngủi\\nubiquitous - khắp nơi",
                  "cards": [
                    {"term": "ephemeral", "definition": "ngắn ngủi"},
                    {"term": "ubiquitous", "definition": "khắp nơi"}
                  ],
                  "confidence": 0.92,
                  "engine": "gemini_vision",
                  "warnings": []
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/ocr/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fastApiJson, MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vocab.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        OcrExtractResponse response = aiOcrClient.extractCards(file, "en", "vi");

        assertThat(response.getRawText()).contains("ephemeral");
        assertThat(response.getEngine()).isEqualTo("gemini_vision");
        assertThat(response.getConfidence()).isEqualTo(0.92);
        assertThat(response.getCards()).hasSize(2);
        assertThat(response.getCards().get(0).getTerm()).isEqualTo("ephemeral");
        assertThat(response.getCards().get(0).getDefinition()).isEqualTo("ngắn ngủi");
        mockServer.verify();
    }

    @Test
    void extractCards_serverError_throwsAiServiceUnavailable() {
        mockServer.expect(requestTo("http://localhost:8000/ocr/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vocab.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> aiOcrClient.extractCards(file, null, null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));

        mockServer.verify();
    }

    @Test
    void extractCards_connectionFailure_throwsAiServiceUnavailable() {
        mockServer.expect(requestTo("http://localhost:8000/ocr/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection refused");
                });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vocab.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> aiOcrClient.extractCards(file, null, null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));
    }
}
