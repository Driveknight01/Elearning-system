package com.elearning.module.flashcard.service;

import com.elearning.common.dto.PageResponse;
import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.util.ShareTokenGenerator;
import com.elearning.module.flashcard.dto.FolderDetailResponse;
import com.elearning.module.flashcard.dto.FolderRequest;
import com.elearning.module.flashcard.dto.FolderSummaryResponse;
import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.Folder;
import com.elearning.module.flashcard.entity.FolderSet;
import com.elearning.module.flashcard.entity.FolderSetId;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FolderRepository;
import com.elearning.module.flashcard.repository.FolderSetRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for folder CRUD, visibility control, and adding/removing flashcard sets.
 */
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderSetRepository folderSetRepository;
    private final UserRepository userRepository;
    private final FlashcardSetService flashcardSetService;

    @Transactional
    public FolderDetailResponse createFolder(Long ownerId, FolderRequest request) {
        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Folder folder = Folder.builder()
                .owner(owner)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .shareToken(generateUniqueShareToken())
                .build();

        Folder saved = folderRepository.saveAndFlush(folder);
        return toDetailResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<FolderSummaryResponse> getMyFolders(Long ownerId, Pageable pageable) {
        Page<Folder> page = folderRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
        List<FolderSummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public FolderDetailResponse getFolderById(Long folderId, String shareToken, UserPrincipal currentUser) {
        Folder folder = findFolderOrThrow(folderId);
        boolean isOwnerOrAdmin = isOwnerOrAdmin(folder, currentUser);
        assertCanView(folder, shareToken, isOwnerOrAdmin);

        boolean isOwner = currentUser != null && folder.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(folder, isOwner);
    }

    @Transactional(readOnly = true)
    public FolderDetailResponse getFolderByShareToken(String shareToken, UserPrincipal currentUser) {
        Folder folder = folderRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thư mục"));
        boolean isOwnerOrAdmin = isOwnerOrAdmin(folder, currentUser);
        assertCanView(folder, shareToken, isOwnerOrAdmin);

        boolean isOwner = currentUser != null && folder.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(folder, isOwner);
    }

    @Transactional
    public FolderDetailResponse updateFolder(Long folderId, UserPrincipal currentUser, FolderRequest request) {
        Folder folder = findFolderOrThrow(folderId);
        if (!isOwnerOrAdmin(folder, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền chỉnh sửa thư mục này");
        }

        folder.setName(request.getName().trim());
        folder.setDescription(trimToNull(request.getDescription()));
        if (request.getVisibility() != null) {
            folder.setVisibility(request.getVisibility());
        }
        if (request.isRegenerateShareToken()) {
            folder.setShareToken(generateUniqueShareToken());
        }

        Folder saved = folderRepository.saveAndFlush(folder);
        boolean isOwner = currentUser != null && folder.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(saved, isOwner);
    }

    @Transactional
    public void deleteFolder(Long folderId, UserPrincipal currentUser) {
        Folder folder = findFolderOrThrow(folderId);
        if (!isOwnerOrAdmin(folder, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa thư mục này");
        }
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderDetailResponse addSetToFolder(Long folderId, Long setId, String shareToken, UserPrincipal currentUser) {
        Folder folder = findFolderOrThrow(folderId);
        if (!isOwnerOrAdmin(folder, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thêm bộ thẻ vào thư mục này");
        }

        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        if (!folderSetRepository.existsByIdFolderIdAndIdSetId(folderId, setId)) {
            folderSetRepository.saveAndFlush(FolderSet.builder()
                    .id(new FolderSetId(folderId, setId))
                    .folder(folder)
                    .flashcardSet(set)
                    .build());
        }

        return toDetailResponse(findFolderOrThrow(folderId), true);
    }

    @Transactional
    public FolderDetailResponse removeSetFromFolder(Long folderId, Long setId, UserPrincipal currentUser) {
        Folder folder = findFolderOrThrow(folderId);
        if (!isOwnerOrAdmin(folder, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa bộ thẻ khỏi thư mục này");
        }

        folderSetRepository.deleteByIdFolderIdAndIdSetId(folderId, setId);
        return toDetailResponse(findFolderOrThrow(folderId), true);
    }

    private Folder findFolderOrThrow(Long folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thư mục"));
    }

    private void assertCanView(Folder folder, String shareToken, boolean isOwnerOrAdmin) {
        if (isOwnerOrAdmin) {
            return;
        }
        switch (folder.getVisibility()) {
            case PUBLIC -> {
                // Publicly accessible
            }
            case LINK_ONLY -> {
                if (shareToken == null || !shareToken.equals(folder.getShareToken())) {
                    throw new AppException(ErrorCode.FORBIDDEN, "Cần liên kết chia sẻ hợp lệ để xem thư mục này");
                }
            }
            case PRIVATE -> throw new AppException(ErrorCode.FORBIDDEN, "Thư mục này đang ở chế độ riêng tư");
        }
    }

    private boolean isOwnerOrAdmin(Folder folder, UserPrincipal currentUser) {
        if (currentUser == null) {
            return false;
        }
        return folder.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole() == Role.ADMIN;
    }

    private String generateUniqueShareToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = ShareTokenGenerator.generate();
            if (!folderRepository.existsByShareToken(candidate)) {
                return candidate;
            }
        }
        return ShareTokenGenerator.generate(16);
    }

    private FolderSummaryResponse toSummaryResponse(Folder folder) {
        return FolderSummaryResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .visibility(folder.getVisibility())
                .shareToken(folder.getShareToken())
                .setCount(folderSetRepository.countByIdFolderId(folder.getId()))
                .owner(toOwnerSummary(folder.getOwner()))
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    private FolderDetailResponse toDetailResponse(Folder folder, boolean ownerView) {
        List<FolderSet> links = folderSetRepository.findAllByIdFolderIdOrderByAddedAtDesc(folder.getId());
        List<SetSummaryResponse> sets = links.stream()
                .map(FolderSet::getFlashcardSet)
                .filter(s -> !s.isDeleted())
                .map(flashcardSetService::toSummaryResponse)
                .toList();

        return FolderDetailResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .visibility(folder.getVisibility())
                .shareToken(folder.getShareToken())
                .setCount(sets.size())
                .ownerView(ownerView)
                .owner(toOwnerSummary(folder.getOwner()))
                .sets(sets)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    private SetSummaryResponse.OwnerSummary toOwnerSummary(User owner) {
        return SetSummaryResponse.OwnerSummary.builder()
                .id(owner.getId())
                .username(owner.getUsername())
                .fullName(owner.getFullName())
                .avatarUrl(owner.getAvatarUrl())
                .verifiedTeacher(owner.isVerifiedTeacher())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
