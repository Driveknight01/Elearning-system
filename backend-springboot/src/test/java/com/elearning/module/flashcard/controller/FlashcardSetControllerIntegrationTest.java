package com.elearning.module.flashcard.controller;

import com.elearning.module.admin.entity.Category;
import com.elearning.module.admin.repository.CategoryRepository;
import com.elearning.module.flashcard.dto.CreateFlashcardSetRequest;
import com.elearning.module.flashcard.dto.FlashcardItemRequest;
import com.elearning.module.flashcard.dto.UpdateFlashcardSetRequest;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlashcardSetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FlashcardSetRepository flashcardSetRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User otherUser;
    private User admin;
    private Category category;
    private String ownerJwt;
    private String otherJwt;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        flashcardSetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .username("set_owner")
                .email("owner@smartflash.vn")
                .fullName("Set Owner")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build());

        otherUser = userRepository.save(User.builder()
                .username("other_learner")
                .email("other@smartflash.vn")
                .fullName("Other Learner")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        admin = userRepository.save(User.builder()
                .username("sys_admin")
                .email("admin@smartflash.vn")
                .fullName("System Admin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Computer Science")
                .slug("computer-science")
                .colorHex("#4255FF")
                .displayOrder(1)
                .active(true)
                .build());

        ownerJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(owner));
        otherJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(otherUser));
        adminJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(admin));
    }

    @Test
    void createUpdateAndSoftDeleteSet_shouldSyncCardsAndEnforceOwnership() throws Exception {
        CreateFlashcardSetRequest createRequest = CreateFlashcardSetRequest.builder()
                .title("Spring Boot Fundamentals")
                .description("Core beans and annotations")
                .categoryId(category.getId())
                .visibility(Visibility.PUBLIC)
                .termLang("en")
                .defLang("vi")
                .tags(List.of("spring", "java", "spring"))
                .cards(List.of(
                        FlashcardItemRequest.builder().term("@Service").definition("Business logic bean").build(),
                        FlashcardItemRequest.builder().term("@Repository").definition("Data access bean").build()
                ))
                .build();

        // Guest cannot create
        mockMvc.perform(post("/api/v1/flashcard-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());

        MvcResult createResult = mockMvc.perform(post("/api/v1/flashcard-sets")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Fundamentals"))
                .andExpect(jsonPath("$.data.cardCount").value(2))
                .andExpect(jsonPath("$.data.shareToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.data.cards", hasSize(2)))
                .andReturn();

        JsonNode createdData = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        long setId = createdData.get("id").asLong();
        long firstCardId = createdData.get("cards").get(0).get("id").asLong();
        String originalShareToken = createdData.get("shareToken").asText();

        // Non-owner cannot update
        UpdateFlashcardSetRequest updateRequest = UpdateFlashcardSetRequest.builder()
                .title("Spring Boot Updated")
                .description("Updated description")
                .categoryId(category.getId())
                .visibility(Visibility.LINK_ONLY)
                .regenerateShareToken(true)
                .cards(List.of(
                        FlashcardItemRequest.builder()
                                .id(firstCardId)
                                .term("@Service Updated")
                                .definition("Updated service definition")
                                .build(),
                        FlashcardItemRequest.builder()
                                .term("@RestController")
                                .definition("REST API controller")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/flashcard-sets/" + setId)
                        .header("Authorization", "Bearer " + otherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_403"));

        // Owner updates: keeps firstCardId, removes second card, adds @RestController, rotates shareToken
        MvcResult updateResult = mockMvc.perform(put("/api/v1/flashcard-sets/" + setId)
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Updated"))
                .andExpect(jsonPath("$.data.visibility").value("LINK_ONLY"))
                .andExpect(jsonPath("$.data.cardCount").value(2))
                .andExpect(jsonPath("$.data.cards[0].id").value(firstCardId))
                .andExpect(jsonPath("$.data.cards[0].term").value("@Service Updated"))
                .andExpect(jsonPath("$.data.cards[1].term").value("@RestController"))
                .andReturn();

        String rotatedShareToken = objectMapper.readTree(updateResult.getResponse().getContentAsString())
                .get("data").get("shareToken").asText();
        assertThat(rotatedShareToken).isNotEqualTo(originalShareToken);

        // Non-owner cannot delete
        mockMvc.perform(delete("/api/v1/flashcard-sets/" + setId)
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isForbidden());

        // Owner soft-deletes
        mockMvc.perform(delete("/api/v1/flashcard-sets/" + setId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        FlashcardSet deletedSet = flashcardSetRepository.findById(setId).orElseThrow();
        assertThat(deletedSet.isDeleted()).isTrue();

        mockMvc.perform(get("/api/v1/flashcard-sets/" + setId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SET_404"));
    }

    @Test
    void visibilityAccessControl_shouldEnforcePublicLinkOnlyAndPrivateRules() throws Exception {
        long publicSetId = createSet("Public Set", Visibility.PUBLIC);
        long linkOnlySetId = createSet("Link Only Set", Visibility.LINK_ONLY);
        long privateSetId = createSet("Private Set", Visibility.PRIVATE);

        String linkOnlyToken = flashcardSetRepository.findById(linkOnlySetId).orElseThrow().getShareToken();
        String privateToken = flashcardSetRepository.findById(privateSetId).orElseThrow().getShareToken();

        // PUBLIC: Guest can access and viewCount increments
        mockMvc.perform(get("/api/v1/flashcard-sets/" + publicSetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // LINK_ONLY: Guest without token or wrong token -> 403
        mockMvc.perform(get("/api/v1/flashcard-sets/" + linkOnlySetId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_403"));

        mockMvc.perform(get("/api/v1/flashcard-sets/" + linkOnlySetId).param("token", "wrong-token"))
                .andExpect(status().isForbidden());

        // LINK_ONLY: Guest with valid ?token= or /share/{token} -> 200
        mockMvc.perform(get("/api/v1/flashcard-sets/" + linkOnlySetId).param("token", linkOnlyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Link Only Set"));

        mockMvc.perform(get("/api/v1/flashcard-sets/share/" + linkOnlyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(linkOnlySetId));

        // PRIVATE: Other user blocked even with shareToken -> 403
        mockMvc.perform(get("/api/v1/flashcard-sets/" + privateSetId)
                        .header("Authorization", "Bearer " + otherJwt)
                        .param("token", privateToken))
                .andExpect(status().isForbidden());

        // PRIVATE: Owner and Admin allowed -> 200
        mockMvc.perform(get("/api/v1/flashcard-sets/" + privateSetId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/flashcard-sets/" + privateSetId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());

        // /my requires auth and returns all 3 sets of owner
        mockMvc.perform(get("/api/v1/flashcard-sets/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/flashcard-sets/my")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    private long createSet(String title, Visibility visibility) throws Exception {
        CreateFlashcardSetRequest req = CreateFlashcardSetRequest.builder()
                .title(title)
                .visibility(visibility)
                .cards(List.of(FlashcardItemRequest.builder()
                        .term("Term 1")
                        .definition("Definition 1")
                        .build()))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/flashcard-sets")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }
}
