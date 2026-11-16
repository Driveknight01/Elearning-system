package com.elearning.module.flashcard.controller;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.flashcard.dto.OcrExtractResponse;
import com.elearning.module.flashcard.dto.OcrExtractedCardDto;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.flashcard.service.AiOcrClient;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.JwtTokenProvider;
import com.elearning.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlashcardSetOcrIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlashcardSetRepository flashcardSetRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AiOcrClient aiOcrClient;

    private String learnerJwt;

    @BeforeEach
    void setUp() {
        flashcardSetRepository.deleteAll();
        userRepository.deleteAll();

        User learner = userRepository.save(User.builder()
                .username("ocr_learner")
                .email("ocr_learner@smartflash.vn")
                .fullName("OCR Learner")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        learnerJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(learner));
    }

    @Test
    void ocrExtract_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/flashcard-sets/ocr-extract").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ocrExtract_invalidFileType_returns400() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello - world".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/flashcard-sets/ocr-extract")
                        .file(textFile)
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    @Test
    void ocrExtract_emptyFile_returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/flashcard-sets/ocr-extract")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    @Test
    void ocrExtract_validImage_returnsParsedCards() throws Exception {
        OcrExtractResponse mockedResult = OcrExtractResponse.builder()
                .rawText("algorithm - thuật toán\nrecursion - đệ quy")
                .cards(List.of(
                        OcrExtractedCardDto.builder().term("algorithm").definition("thuật toán").build(),
                        OcrExtractedCardDto.builder().term("recursion").definition("đệ quy").build()
                ))
                .confidence(0.88)
                .engine("tesseract")
                .warnings(List.of())
                .build();

        when(aiOcrClient.extractCards(any(), eq("en"), eq("vi"))).thenReturn(mockedResult);

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "textbook.png", "image/png", new byte[]{10, 20, 30, 40}
        );

        mockMvc.perform(multipart("/api/v1/flashcard-sets/ocr-extract")
                        .file(imageFile)
                        .param("sourceLang", "en")
                        .param("targetLang", "vi")
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.engine").value("tesseract"))
                .andExpect(jsonPath("$.data.confidence").value(0.88))
                .andExpect(jsonPath("$.data.cards", hasSize(2)))
                .andExpect(jsonPath("$.data.cards[0].term").value("algorithm"))
                .andExpect(jsonPath("$.data.cards[0].definition").value("thuật toán"));
    }

    @Test
    void ocrExtract_aiServiceUnavailable_returns503() throws Exception {
        when(aiOcrClient.extractCards(any(), any(), any()))
                .thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Dịch vụ OCR AI tạm thời không khả dụng"));

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "textbook.jpg", "image/jpeg", new byte[]{10, 20, 30}
        );

        mockMvc.perform(multipart("/api/v1/flashcard-sets/ocr-extract")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + learnerJwt))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AI_503"));
    }
}
