package com.elearning.module.flashcard.controller;

import com.elearning.module.admin.entity.Category;
import com.elearning.module.admin.repository.CategoryRepository;
import com.elearning.module.flashcard.dto.CloneSetRequest;
import com.elearning.module.flashcard.dto.RateSetRequest;
import com.elearning.module.flashcard.dto.ResumeStateRequest;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.StudyMode;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.flashcard.repository.StarredCardRepository;
import com.elearning.module.flashcard.repository.UserSetInteractionRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.JwtTokenProvider;
import com.elearning.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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
class SetExploreCloneAndInteractionIntegrationTest {

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
    private UserSetInteractionRepository userSetInteractionRepository;

    @Autowired
    private StarredCardRepository starredCardRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User author;
    private User learner;
    private String learnerJwt;
    private Category englishCategory;
    private FlashcardSet publicSet;
    private FlashcardSet privateSet;
    private Flashcard card1;

    @BeforeEach
    void setUp() {
        starredCardRepository.deleteAll();
        userSetInteractionRepository.deleteAll();
        flashcardSetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        author = userRepository.save(User.builder()
                .username("author_minh")
                .email("minh@smartflash.vn")
                .fullName("Minh Le")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build());

        learner = userRepository.save(User.builder()
                .username("learner_hoa")
                .email("hoa@smartflash.vn")
                .fullName("Hoa Pham")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        learnerJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(learner));

        englishCategory = categoryRepository.save(Category.builder()
                .name("English")
                .slug("english")
                .colorHex("#4255FF")
                .displayOrder(1)
                .active(true)
                .build());

        FlashcardSet set = FlashcardSet.builder()
                .owner(author)
                .category(englishCategory)
                .title("IELTS Academic Collocations")
                .description("High-band speaking and writing phrases")
                .visibility(Visibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .featured(true)
                .shareToken("pub_tok_ielts_1")
                .viewCount(120)
                .cardCount(2)
                .build();
        card1 = Flashcard.builder().term("mitigate").definition("make less severe").orderIndex(0).build();
        Flashcard card2 = Flashcard.builder().term("exacerbate").definition("make a problem worse").orderIndex(1).build();
        set.addCard(card1);
        set.addCard(card2);
        publicSet = flashcardSetRepository.saveAndFlush(set);
        card1 = publicSet.getCards().get(0);

        FlashcardSet priv = FlashcardSet.builder()
                .owner(author)
                .category(englishCategory)
                .title("Secret Draft Set")
                .visibility(Visibility.PRIVATE)
                .moderationStatus(ModerationStatus.APPROVED)
                .shareToken("priv_tok_secret_1")
                .cardCount(1)
                .build();
        priv.addCard(Flashcard.builder().term("hidden").definition("secret").orderIndex(0).build());
        privateSet = flashcardSetRepository.saveAndFlush(priv);
    }

    @Test
    void exploreAndSearchPublicSets_shouldExcludePrivateSetsAndFilterByKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/flashcard-sets/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredSets", hasSize(1)))
                .andExpect(jsonPath("$.data.popularSets", hasSize(1)))
                .andExpect(jsonPath("$.data.categories", hasSize(1)));

        mockMvc.perform(get("/api/v1/flashcard-sets/search")
                        .param("keyword", "collocations")
                        .param("categoryId", String.valueOf(englishCategory.getId()))
                        .param("sort", "popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("IELTS Academic Collocations"));

        // Private set must never appear in public search
        mockMvc.perform(get("/api/v1/flashcard-sets/search")
                        .param("keyword", "Secret Draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void cloneSet_shouldDuplicateCardsAndIncrementCloneCount() throws Exception {
        CloneSetRequest req = CloneSetRequest.builder()
                .title("My IELTS Fork")
                .visibility(Visibility.PRIVATE)
                .build();

        mockMvc.perform(post("/api/v1/flashcard-sets/" + publicSet.getId() + "/clone")
                        .header("Authorization", "Bearer " + learnerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("My IELTS Fork"))
                .andExpect(jsonPath("$.data.clonedFromSetId").value(publicSet.getId()))
                .andExpect(jsonPath("$.data.cardCount").value(2))
                .andExpect(jsonPath("$.data.cards", hasSize(2)));

        FlashcardSet reloadedSource = flashcardSetRepository.findById(publicSet.getId()).orElseThrow();
        assertThat(reloadedSource.getCloneCount()).isEqualTo(1);

        // Cannot clone another user's PRIVATE set
        mockMvc.perform(post("/api/v1/flashcard-sets/" + privateSet.getId() + "/clone")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void interactionsAndStarredCards_shouldPersistSavedRecentsResumeRatingAndStars() throws Exception {
        // 1. Save set & verify /saved
        mockMvc.perform(post("/api/v1/flashcard-sets/" + publicSet.getId() + "/save")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true));

        mockMvc.perform(get("/api/v1/flashcard-sets/saved")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 2. Save cross-platform resume state & verify /interaction and /recents
        ResumeStateRequest resumeReq = ResumeStateRequest.builder()
                .studyMode(StudyMode.LEARN)
                .resumeStateJson(Map.of("currentCardIndex", 1, "learnRound", 2))
                .build();

        mockMvc.perform(put("/api/v1/flashcard-sets/" + publicSet.getId() + "/resume")
                        .header("Authorization", "Bearer " + learnerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastStudyMode").value("LEARN"))
                .andExpect(jsonPath("$.data.resumeStateJson.currentCardIndex").value(1))
                .andExpect(jsonPath("$.data.resumeStateJson.learnRound").value(2));

        mockMvc.perform(get("/api/v1/flashcard-sets/recents")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 3. Rate set 5 stars & verify avgRating updated on FlashcardSet
        RateSetRequest rateReq = RateSetRequest.builder()
                .rating((short) 5)
                .reviewComment("Excellent collocations!")
                .build();

        mockMvc.perform(post("/api/v1/flashcard-sets/" + publicSet.getId() + "/rate")
                        .header("Authorization", "Bearer " + learnerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5));

        FlashcardSet ratedSet = flashcardSetRepository.findById(publicSet.getId()).orElseThrow();
        assertThat(ratedSet.getRatingCount()).isEqualTo(1);
        assertThat(ratedSet.getAvgRating()).isEqualByComparingTo("5.00");

        mockMvc.perform(get("/api/v1/flashcard-sets/" + publicSet.getId() + "/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].reviewComment").value("Excellent collocations!"));

        // 4. Unsave set
        mockMvc.perform(delete("/api/v1/flashcard-sets/" + publicSet.getId() + "/save")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(false));

        // 5. Star & Unstar difficult card
        mockMvc.perform(post("/api/v1/flashcard-sets/" + publicSet.getId() + "/cards/" + card1.getId() + "/star")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/flashcard-sets/" + publicSet.getId() + "/starred-cards")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.starredCardIds", hasSize(1)))
                .andExpect(jsonPath("$.data.cards[0].term").value("mitigate"));

        mockMvc.perform(delete("/api/v1/flashcard-sets/" + publicSet.getId() + "/cards/" + card1.getId() + "/star")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/flashcard-sets/" + publicSet.getId() + "/starred-cards")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.starredCardIds", hasSize(0)));
    }
}
