package com.elearning.module.flashcard.repository;

import com.elearning.module.admin.entity.Category;
import com.elearning.module.admin.repository.CategoryRepository;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.Folder;
import com.elearning.module.flashcard.entity.FolderSet;
import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.SourceType;
import com.elearning.module.flashcard.entity.StarredCard;
import com.elearning.module.flashcard.entity.StudyMode;
import com.elearning.module.flashcard.entity.UserSetInteraction;
import com.elearning.module.flashcard.entity.UserSetInteractionId;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.group.entity.StudyGroup;
import com.elearning.module.group.entity.StudyGroupMember;
import com.elearning.module.group.entity.StudyGroupRole;
import com.elearning.module.group.entity.StudyGroupSet;
import com.elearning.module.group.repository.StudyGroupMemberRepository;
import com.elearning.module.group.repository.StudyGroupRepository;
import com.elearning.module.group.repository.StudyGroupSetRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FlashcardAndGroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FlashcardSetRepository flashcardSetRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private UserSetInteractionRepository userSetInteractionRepository;

    @Autowired
    private StarredCardRepository starredCardRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderSetRepository folderSetRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private StudyGroupMemberRepository studyGroupMemberRepository;

    @Autowired
    private StudyGroupSetRepository studyGroupSetRepository;

    private User owner;
    private User learner;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .username("teacher_linh")
                .email("linh@smartflash.vn")
                .fullName("Linh Nguyen")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build());

        learner = userRepository.save(User.builder()
                .username("student_nam")
                .email("nam@smartflash.vn")
                .fullName("Nam Tran")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .name("English IELTS")
                .slug("english-ielts")
                .colorHex("#4255FF")
                .displayOrder(1)
                .active(true)
                .build());
    }

    @Test
    void flashcardSetAndCards_shouldPersistTagsArrayAndSearchPublicSets() {
        FlashcardSet set = FlashcardSet.builder()
                .owner(owner)
                .category(category)
                .title("IELTS Band 8 Vocabulary")
                .description("Essential academic collocations")
                .visibility(Visibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .shareToken("share_tok_ielts_001")
                .tags(List.of("ielts", "academic", "vocabulary"))
                .sourceType(SourceType.MANUAL)
                .cardCount(2)
                .build();

        set.addCard(Flashcard.builder()
                .term("ubiquitous")
                .definition("present, appearing, or found everywhere")
                .orderIndex(0)
                .build());
        set.addCard(Flashcard.builder()
                .term("ephemeral")
                .definition("lasting for a very short time")
                .orderIndex(1)
                .build());

        FlashcardSet saved = flashcardSetRepository.saveAndFlush(set);
        entityManager.clear();

        FlashcardSet reloaded = flashcardSetRepository.findByIdAndDeletedAtIsNull(saved.getId()).orElseThrow();
        assertThat(reloaded.getTags()).containsExactly("ielts", "academic", "vocabulary");
        assertThat(reloaded.getAvgRating()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(flashcardRepository.findAllByFlashcardSetIdOrderByOrderIndexAsc(reloaded.getId()))
                .extracting(Flashcard::getTerm)
                .containsExactly("ubiquitous", "ephemeral");

        Page<FlashcardSet> searchResult = flashcardSetRepository.searchPublicSets(
                "band 8",
                category.getId(),
                Visibility.PUBLIC,
                ModerationStatus.APPROVED,
                PageRequest.of(0, 10)
        );
        assertThat(searchResult.getTotalElements()).isEqualTo(1);
    }

    @Test
    void userSetInteractionAndStarredCard_shouldPersistCompositeKeyAndResumeJsonb() {
        FlashcardSet set = flashcardSetRepository.save(FlashcardSet.builder()
                .owner(owner)
                .title("Java Spring Boot Core")
                .visibility(Visibility.PUBLIC)
                .shareToken("share_tok_spring_01")
                .cardCount(1)
                .build());

        Flashcard card = flashcardRepository.save(Flashcard.builder()
                .flashcardSet(set)
                .term("IoC Container")
                .definition("Manages bean lifecycle and dependency injection")
                .orderIndex(0)
                .build());

        UserSetInteraction interaction = UserSetInteraction.builder()
                .user(learner)
                .flashcardSet(set)
                .saved(true)
                .lastStudyMode(StudyMode.FLASHCARDS)
                .lastAccessedAt(Instant.now())
                .resumeStateJson(Map.of("currentCardIndex", 18, "learnRound", 2))
                .rating((short) 5)
                .reviewComment("Very helpful set!")
                .ratedAt(Instant.now())
                .build();

        userSetInteractionRepository.saveAndFlush(interaction);
        starredCardRepository.saveAndFlush(StarredCard.builder()
                .user(learner)
                .flashcard(card)
                .build());
        entityManager.clear();

        UserSetInteraction reloadedInteraction = userSetInteractionRepository
                .findById(new UserSetInteractionId(learner.getId(), set.getId()))
                .orElseThrow();

        assertThat(reloadedInteraction.isSaved()).isTrue();
        assertThat(reloadedInteraction.getLastStudyMode()).isEqualTo(StudyMode.FLASHCARDS);
        assertThat(reloadedInteraction.getRating()).isEqualTo((short) 5);
        assertThat(reloadedInteraction.getResumeStateJson())
                .containsEntry("currentCardIndex", 18)
                .containsEntry("learnRound", 2);
        assertThat(userSetInteractionRepository.findAverageRatingBySetId(set.getId())).isEqualTo(5.0);

        // Update existing interaction to confirm Persistable merge works seamlessly
        reloadedInteraction.setResumeStateJson(Map.of("currentCardIndex", 25, "learnRound", 3));
        userSetInteractionRepository.saveAndFlush(reloadedInteraction);
        entityManager.clear();

        UserSetInteraction updated = userSetInteractionRepository
                .findByIdUserIdAndIdSetId(learner.getId(), set.getId())
                .orElseThrow();
        assertThat(updated.getResumeStateJson()).containsEntry("currentCardIndex", 25);

        assertThat(starredCardRepository.existsByIdUserIdAndIdCardId(learner.getId(), card.getId())).isTrue();
        assertThat(starredCardRepository.countByIdUserIdAndFlashcardFlashcardSetId(learner.getId(), set.getId()))
                .isEqualTo(1L);
    }

    @Test
    void folderAndStudyGroup_shouldPersistJoinTablesAndQueries() {
        FlashcardSet set = flashcardSetRepository.save(FlashcardSet.builder()
                .owner(owner)
                .title("JLPT N3 Kanji")
                .visibility(Visibility.LINK_ONLY)
                .shareToken("share_tok_kanji_01")
                .build());

        Folder folder = folderRepository.save(Folder.builder()
                .owner(owner)
                .name("Japanese N3")
                .visibility(Visibility.PUBLIC)
                .shareToken("folder_tok_n3_01")
                .build());

        folderSetRepository.saveAndFlush(FolderSet.builder()
                .folder(folder)
                .flashcardSet(set)
                .build());

        StudyGroup group = studyGroupRepository.save(StudyGroup.builder()
                .creator(owner)
                .name("N3 Evening Group")
                .description("Practice kanji daily")
                .inviteCode("GRP-N3-2026")
                .build());

        studyGroupMemberRepository.save(StudyGroupMember.builder()
                .studyGroup(group)
                .user(owner)
                .role(StudyGroupRole.OWNER)
                .build());
        studyGroupMemberRepository.save(StudyGroupMember.builder()
                .studyGroup(group)
                .user(learner)
                .role(StudyGroupRole.MEMBER)
                .build());
        studyGroupSetRepository.saveAndFlush(StudyGroupSet.builder()
                .studyGroup(group)
                .flashcardSet(set)
                .sharedBy(owner)
                .build());
        entityManager.clear();

        assertThat(folderSetRepository.existsByIdFolderIdAndIdSetId(folder.getId(), set.getId())).isTrue();
        assertThat(folderSetRepository.countByIdFolderId(folder.getId())).isEqualTo(1L);
        assertThat(studyGroupRepository.findByInviteCode("GRP-N3-2026")).isPresent();
        assertThat(studyGroupRepository.findAllByMemberUserId(learner.getId())).hasSize(1);
        assertThat(studyGroupMemberRepository.countByIdGroupId(group.getId())).isEqualTo(2L);
        assertThat(studyGroupSetRepository.existsByIdGroupIdAndIdSetId(group.getId(), set.getId())).isTrue();
    }
}
