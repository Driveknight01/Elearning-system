package com.elearning.common;

import com.elearning.common.dto.ApiResponse;
import com.elearning.common.dto.PageResponse;
import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnSuccessWrapper() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data").value("hello"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleAppException() throws Exception {
        mockMvc.perform(get("/test/app-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SET_404"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy bộ thẻ"));
    }

    @Test
    void shouldHandleValidationException() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REQ_400_VALIDATION"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void shouldBuildPageResponseFromSpringPage() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);
        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.getContent()).containsExactly("a", "b");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @RestController
    @RequestMapping("/test")
    static class DummyController {

        @GetMapping("/ok")
        ApiResponse<String> ok() {
            return ApiResponse.success("hello");
        }

        @GetMapping("/app-error")
        ApiResponse<Void> appError() {
            throw new AppException(ErrorCode.SET_NOT_FOUND);
        }

        @PostMapping("/validate")
        ApiResponse<Void> validate(@Valid @RequestBody DummyRequest request) {
            return ApiResponse.successMessage("OK");
        }
    }

    record DummyRequest(@NotBlank(message = "Title is required") String title) {
    }
}
