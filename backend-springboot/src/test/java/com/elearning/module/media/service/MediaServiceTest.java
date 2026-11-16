package com.elearning.module.media.service;

import com.elearning.common.service.LocalStorageService;
import com.elearning.config.MediaProperties;
import com.elearning.module.flashcard.repository.FlashcardRepository;
import com.elearning.module.media.dto.StockImageSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private LocalStorageService localStorageService;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Test
    void searchStockImages_withPixabayKey_parsesHits() {
        MediaProperties props = new MediaProperties();
        props.setPixabayApiKey("test-pixabay-key");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        MediaService mediaService = new MediaService(props, localStorageService, flashcardRepository, builder.build());

        String pixabayJson = """
                {
                  "totalHits": 1,
                  "hits": [
                    {
                      "id": 1001,
                      "previewURL": "https://cdn.pixabay.com/photo/preview.jpg",
                      "webformatURL": "https://cdn.pixabay.com/photo/web.jpg",
                      "webformatWidth": 640,
                      "webformatHeight": 480,
                      "user": "alice"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("https://pixabay.com/api/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pixabayJson, MediaType.APPLICATION_JSON));

        StockImageSearchResponse result = mediaService.searchStockImages("photosynthesis", 5);

        assertThat(result.getProvider()).isEqualTo("pixabay");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("https://cdn.pixabay.com/photo/web.jpg");
        assertThat(result.getItems().get(0).getAttribution()).contains("alice");
        mockServer.verify();
    }

    @Test
    void searchStockImages_withUnsplashKey_parsesResults() {
        MediaProperties props = new MediaProperties();
        props.setUnsplashAccessKey("test-unsplash-key");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        MediaService mediaService = new MediaService(props, localStorageService, flashcardRepository, builder.build());

        String unsplashJson = """
                {
                  "results": [
                    {
                      "id": "u-42",
                      "width": 1200,
                      "height": 800,
                      "urls": {
                        "small": "https://images.unsplash.com/small.jpg",
                        "regular": "https://images.unsplash.com/regular.jpg"
                      },
                      "user": {"name": "Bob"}
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("https://api.unsplash.com/search/photos")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Client-ID test-unsplash-key"))
                .andRespond(withSuccess(unsplashJson, MediaType.APPLICATION_JSON));

        StockImageSearchResponse result = mediaService.searchStockImages("galaxy", 5);

        assertThat(result.getProvider()).isEqualTo("unsplash");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("https://images.unsplash.com/regular.jpg");
        assertThat(result.getItems().get(0).getAttribution()).contains("Bob");
        mockServer.verify();
    }

    @Test
    void searchStockImages_withoutApiKeys_returnsFallbackSuggestions() {
        MediaProperties props = new MediaProperties();
        RestClient.Builder builder = RestClient.builder();
        MediaService mediaService = new MediaService(props, localStorageService, flashcardRepository, builder.build());

        StockImageSearchResponse result = mediaService.searchStockImages("volcano", 6);

        assertThat(result.getProvider()).isEqualTo("fallback");
        assertThat(result.getItems()).hasSize(4);
        assertThat(result.getItems().get(0).getImageUrl()).contains("image.pollinations.ai/prompt/");
    }
}
