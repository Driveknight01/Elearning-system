package com.elearning.module.flashcard.controller;

import com.elearning.module.flashcard.dto.FolderRequest;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.flashcard.repository.FolderRepository;
import com.elearning.module.group.dto.CreateStudyGroupRequest;
import com.elearning.module.group.dto.JoinStudyGroupRequest;
import com.elearning.module.group.repository.StudyGroupRepository;
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
class FolderAndStudyGroupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlashcardSetRepository flashcardSetRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User member;
    private String ownerJwt;
    private String memberJwt;
    private FlashcardSet publicSet;

    @BeforeEach
    void setUp() {
        studyGroupRepository.deleteAll();
        folderRepository.deleteAll();
        flashcardSetRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .username("folder_owner")
                .email("owner_org@smartflash.vn")
                .fullName("Owner User")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        member = userRepository.save(User.builder()
                .username("group_member")
                .email("member_org@smartflash.vn")
                .fullName("Member User")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        ownerJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(owner));
        memberJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(member));

        FlashcardSet set = FlashcardSet.builder()
                .owner(owner)
                .title("TOEIC 800 Business")
                .visibility(Visibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .shareToken("org_pub_set_1")
                .cardCount(1)
                .build();
        set.addCard(Flashcard.builder().term("invoice").definition("hóa đơn").orderIndex(0).build());
        publicSet = flashcardSetRepository.saveAndFlush(set);
    }

    @Test
    void folderCrudAndSetManagement_shouldWorkAndEnforceVisibility() throws Exception {
        FolderRequest createReq = FolderRequest.builder()
                .name("TOEIC Prep")
                .description("All TOEIC decks")
                .visibility(Visibility.LINK_ONLY)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/folders")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("TOEIC Prep"))
                .andExpect(jsonPath("$.data.visibility").value("LINK_ONLY"))
                .andReturn();

        JsonNode folderData = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        long folderId = folderData.get("id").asLong();
        String shareToken = folderData.get("shareToken").asText();

        // Add set to folder
        mockMvc.perform(post("/api/v1/folders/" + folderId + "/sets/" + publicSet.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setCount").value(1))
                .andExpect(jsonPath("$.data.sets[0].title").value("TOEIC 800 Business"));

        // Non-owner cannot add set
        mockMvc.perform(post("/api/v1/folders/" + folderId + "/sets/" + publicSet.getId())
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isForbidden());

        // LINK_ONLY blocks guest without token, allows with token
        mockMvc.perform(get("/api/v1/folders/" + folderId))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/folders/share/" + shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sets", hasSize(1)));

        // Update folder to PRIVATE & remove set
        FolderRequest updateReq = FolderRequest.builder()
                .name("TOEIC Private")
                .visibility(Visibility.PRIVATE)
                .build();

        mockMvc.perform(put("/api/v1/folders/" + folderId)
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("TOEIC Private"));

        mockMvc.perform(delete("/api/v1/folders/" + folderId + "/sets/" + publicSet.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setCount").value(0));

        // My folders
        mockMvc.perform(get("/api/v1/folders/my")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // Delete folder
        mockMvc.perform(delete("/api/v1/folders/" + folderId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());
    }

    @Test
    void studyGroupLifecycle_shouldCreateJoinShareAndManageMembers() throws Exception {
        CreateStudyGroupRequest createGroupReq = CreateStudyGroupRequest.builder()
                .name("IELTS Night Owls")
                .description("Study together every evening")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/study-groups")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.members", hasSize(1)))
                .andReturn();

        JsonNode groupData = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        long groupId = groupData.get("id").asLong();
        String inviteCode = groupData.get("inviteCode").asText();

        // Non-member cannot view group
        mockMvc.perform(get("/api/v1/study-groups/" + groupId)
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isForbidden());

        // Join via invite code
        mockMvc.perform(post("/api/v1/study-groups/join")
                        .header("Authorization", "Bearer " + memberJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinStudyGroupRequest(inviteCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.members", hasSize(2)));

        // Member shares set to group
        mockMvc.perform(post("/api/v1/study-groups/" + groupId + "/sets/" + publicSet.getId())
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharedSets", hasSize(1)));

        // My groups for member
        mockMvc.perform(get("/api/v1/study-groups/my")
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].memberCount").value(2))
                .andExpect(jsonPath("$.data[0].setCount").value(1));

        // Member removes shared set & leaves group
        mockMvc.perform(delete("/api/v1/study-groups/" + groupId + "/sets/" + publicSet.getId())
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharedSets", hasSize(0)));

        mockMvc.perform(delete("/api/v1/study-groups/" + groupId + "/members/" + member.getId())
                        .header("Authorization", "Bearer " + memberJwt))
                .andExpect(status().isOk());

        // Owner deletes group
        mockMvc.perform(delete("/api/v1/study-groups/" + groupId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());
    }
}
