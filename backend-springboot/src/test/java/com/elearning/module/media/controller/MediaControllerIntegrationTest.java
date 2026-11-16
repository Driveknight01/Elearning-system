package com.elearning.module.media.controller;

import com.elearning.common.util.ShareTokenGenerator;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FlashcardRepository;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.media.dto.AiImageGenerateRequest;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.JwtTokenProvider;
import com.elearning.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlashcardSetRepository flashcardSetRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String ownerJwt;
    private String otherJwt;
    private Flashcard ownerCard;

    @BeforeEach
    void setUp() {
        flashcardSetRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(User.builder()
                .username("media_owner")
                .email("media_owner@smartflash.vn")
                .fullName("Media Owner")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        User other = userRepository.save(User.builder()
                .username("media_other")
                .email("media_other@smartflash.vn")
                .fullName("Media Other")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        FlashcardSet set = FlashcardSet.builder()
                .owner(owner)
                .title("Nature Vocabulary")
                .visibility(Visibility.PRIVATE)
                .shareToken(ShareTokenGenerator.generate())
                .cardCount(1)
                .build();

        Flashcard card = Flashcard.builder()
                .flashcardSet(set)
                .term("glacier")
                .definition("a slowly moving mass of ice")
                .orderIndex(0)
                .build();
        set.getCards().add(card);
        set = flashcardSetRepository.save(set);
        ownerCard = set.getCards().get(0);

        ownerJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(owner));
        otherJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(other));
    }

    @Test
    void uploadCardImage_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "card.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/media/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadCardImage_withOwnedCardId_persistsImageUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "glacier.png", "image/png", new byte[]{10, 20, 30, 40}
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("cardId", ownerCard.getId().toString())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", containsString("/uploads/cards/")))
                .andExpect(jsonPath("$.data.savedToCardId").value(ownerCard.getId()))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String uploadedUrl = data.get("url").asText();

        Flashcard updated = flashcardRepository.findById(ownerCard.getId()).orElseThrow();
        assertThat(updated.getImageUrl()).isEqualTo(uploadedUrl);
    }

    @Test
    void uploadCardImage_withForeignCardId_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hack.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("cardId", ownerCard.getId().toString())
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_403"));
    }

    @Test
    void searchStockImages_returnsFallbackItemsWhenNoApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/media/stock-search")
                        .param("query", "ocean")
                        .param("perPage", "4")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value("ocean"))
                .andExpect(jsonPath("$.data.provider").value("fallback"))
                .andExpect(jsonPath("$.data.items", hasSize(4)));
    }

    @Test
    void generateAiMnemonicImage_withOwnedCardId_savesPollinationsUrlToCard() throws Exception {
        AiImageGenerateRequest request = AiImageGenerateRequest.builder()
                .term("glacier")
                .definition("a slowly moving mass of ice")
                .style("flat_illustration")
                .cardId(ownerCard.getId())
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/media/ai-generate")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("pollinations"))
                .andExpect(jsonPath("$.data.imageUrl", containsString("image.pollinations.ai/prompt/")))
                .andExpect(jsonPath("$.data.savedToCardId").value(ownerCard.getId()))
                .andReturn();

        String generatedUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("imageUrl").asText();

        Flashcard updated = flashcardRepository.findById(ownerCard.getId()).orElseThrow();
        assertThat(updated.getImageUrl()).isEqualTo(generatedUrl);
    }

    @Test
    void generateAiMnemonicImage_blankTerm_returns400() throws Exception {
        AiImageGenerateRequest request = AiImageGenerateRequest.builder()
                .term("   ")
                .build();

        mockMvc.perform(post("/api/v1/media/ai-generate")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400_VALIDATION"));
    }
}
