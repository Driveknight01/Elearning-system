package com.elearning.module.group.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.util.ShareTokenGenerator;
import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.service.FlashcardSetService;
import com.elearning.module.group.dto.CreateStudyGroupRequest;
import com.elearning.module.group.dto.StudyGroupDetailResponse;
import com.elearning.module.group.dto.StudyGroupMemberResponse;
import com.elearning.module.group.dto.StudyGroupSharedSetResponse;
import com.elearning.module.group.dto.StudyGroupSummaryResponse;
import com.elearning.module.group.entity.StudyGroup;
import com.elearning.module.group.entity.StudyGroupMember;
import com.elearning.module.group.entity.StudyGroupMemberId;
import com.elearning.module.group.entity.StudyGroupRole;
import com.elearning.module.group.entity.StudyGroupSet;
import com.elearning.module.group.entity.StudyGroupSetId;
import com.elearning.module.group.repository.StudyGroupMemberRepository;
import com.elearning.module.group.repository.StudyGroupRepository;
import com.elearning.module.group.repository.StudyGroupSetRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Business logic for peer study groups: creation, invite-code joining, member management,
 * and sharing flashcard sets within a group.
 */
@Service
@RequiredArgsConstructor
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final StudyGroupSetRepository studyGroupSetRepository;
    private final UserRepository userRepository;
    private final FlashcardSetService flashcardSetService;

    @Transactional
    public StudyGroupDetailResponse createGroup(Long creatorId, CreateStudyGroupRequest request) {
        User creator = userRepository.findByIdAndDeletedAtIsNull(creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        StudyGroup group = StudyGroup.builder()
                .creator(creator)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .inviteCode(generateUniqueInviteCode())
                .build();

        StudyGroup saved = studyGroupRepository.saveAndFlush(group);

        studyGroupMemberRepository.saveAndFlush(StudyGroupMember.builder()
                .id(new StudyGroupMemberId(saved.getId(), creator.getId()))
                .studyGroup(saved)
                .user(creator)
                .role(StudyGroupRole.OWNER)
                .build());

        return toDetailResponse(saved, StudyGroupRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<StudyGroupSummaryResponse> getMyGroups(Long userId) {
        return studyGroupRepository.findAllByMemberUserId(userId).stream()
                .map(group -> toSummaryResponse(group, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyGroupDetailResponse getGroupDetail(Long groupId, UserPrincipal currentUser) {
        StudyGroup group = findGroupOrThrow(groupId);
        StudyGroupMember membership = assertMemberOrAdmin(groupId, currentUser);
        return toDetailResponse(group, membership != null ? membership.getRole() : null);
    }

    @Transactional
    public StudyGroupDetailResponse joinByInviteCode(Long userId, String inviteCode) {
        String normalizedCode = inviteCode.trim().toUpperCase(Locale.ROOT);
        StudyGroup group = studyGroupRepository.findByInviteCode(normalizedCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Mã mời nhóm học không hợp lệ"));

        StudyGroupMember existing = studyGroupMemberRepository
                .findByIdGroupIdAndIdUserId(group.getId(), userId)
                .orElse(null);

        if (existing != null) {
            return toDetailResponse(group, existing.getRole());
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        studyGroupMemberRepository.saveAndFlush(StudyGroupMember.builder()
                .id(new StudyGroupMemberId(group.getId(), userId))
                .studyGroup(group)
                .user(user)
                .role(StudyGroupRole.MEMBER)
                .build());

        return toDetailResponse(findGroupOrThrow(group.getId()), StudyGroupRole.MEMBER);
    }

    @Transactional
    public void removeOrLeaveMember(Long groupId, Long targetUserId, UserPrincipal currentUser) {
        StudyGroup group = findGroupOrThrow(groupId);
        boolean isSelf = currentUser.getId().equals(targetUserId);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        StudyGroupMember actorMembership = studyGroupMemberRepository
                .findByIdGroupIdAndIdUserId(groupId, currentUser.getId())
                .orElse(null);

        boolean isOwner = actorMembership != null && actorMembership.getRole() == StudyGroupRole.OWNER;
        if (!isSelf && !isOwner && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa thành viên khỏi nhóm này");
        }

        if (group.getCreator().getId().equals(targetUserId) && !isAdmin) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chủ nhóm không thể tự rời nhóm; hãy xóa nhóm nếu không còn sử dụng");
        }

        studyGroupMemberRepository.deleteByIdGroupIdAndIdUserId(groupId, targetUserId);
    }

    @Transactional
    public void deleteGroup(Long groupId, UserPrincipal currentUser) {
        StudyGroup group = findGroupOrThrow(groupId);
        boolean isOwner = group.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ chủ nhóm mới có quyền xóa nhóm học");
        }
        studyGroupRepository.delete(group);
    }

    @Transactional
    public StudyGroupDetailResponse shareSetToGroup(
            Long groupId,
            Long setId,
            String shareToken,
            UserPrincipal currentUser
    ) {
        StudyGroup group = findGroupOrThrow(groupId);
        StudyGroupMember membership = assertMemberOrAdmin(groupId, currentUser);

        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        if (!studyGroupSetRepository.existsByIdGroupIdAndIdSetId(groupId, setId)) {
            User sharer = userRepository.getReferenceById(currentUser.getId());
            studyGroupSetRepository.saveAndFlush(StudyGroupSet.builder()
                    .id(new StudyGroupSetId(groupId, setId))
                    .studyGroup(group)
                    .flashcardSet(set)
                    .sharedBy(sharer)
                    .build());
        }

        return toDetailResponse(findGroupOrThrow(groupId), membership != null ? membership.getRole() : null);
    }

    @Transactional
    public StudyGroupDetailResponse removeSetFromGroup(Long groupId, Long setId, UserPrincipal currentUser) {
        findGroupOrThrow(groupId);
        StudyGroupMember membership = assertMemberOrAdmin(groupId, currentUser);

        StudyGroupSetId linkId = new StudyGroupSetId(groupId, setId);
        StudyGroupSet link = studyGroupSetRepository.findById(linkId).orElse(null);
        if (link != null) {
            boolean isOwner = membership != null && membership.getRole() == StudyGroupRole.OWNER;
            boolean isSharer = link.getSharedBy().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            if (!isOwner && !isSharer && !isAdmin) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền gỡ bộ thẻ này khỏi nhóm");
            }
            studyGroupSetRepository.deleteByIdGroupIdAndIdSetId(groupId, setId);
        }

        return toDetailResponse(findGroupOrThrow(groupId), membership != null ? membership.getRole() : null);
    }

    private StudyGroup findGroupOrThrow(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhóm học"));
    }

    private StudyGroupMember assertMemberOrAdmin(Long groupId, UserPrincipal currentUser) {
        StudyGroupMember membership = studyGroupMemberRepository
                .findByIdGroupIdAndIdUserId(groupId, currentUser.getId())
                .orElse(null);

        if (membership == null && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không phải thành viên của nhóm học này");
        }
        return membership;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = ShareTokenGenerator.generate(8)
                    .replace('-', 'X')
                    .replace('_', 'Y')
                    .toUpperCase(Locale.ROOT);
            if (!studyGroupRepository.existsByInviteCode(candidate)) {
                return candidate;
            }
        }
        return ShareTokenGenerator.generate(12).replace('-', 'X').replace('_', 'Y').toUpperCase(Locale.ROOT);
    }

    private StudyGroupSummaryResponse toSummaryResponse(StudyGroup group, Long currentUserId) {
        StudyGroupRole myRole = studyGroupMemberRepository
                .findByIdGroupIdAndIdUserId(group.getId(), currentUserId)
                .map(StudyGroupMember::getRole)
                .orElse(null);

        int activeSetCount = (int) studyGroupSetRepository.findAllByIdGroupIdOrderBySharedAtDesc(group.getId())
                .stream()
                .filter(s -> !s.getFlashcardSet().isDeleted())
                .count();

        return StudyGroupSummaryResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .inviteCode(group.getInviteCode())
                .memberCount(studyGroupMemberRepository.countByIdGroupId(group.getId()))
                .setCount(activeSetCount)
                .myRole(myRole)
                .creator(toOwnerSummary(group.getCreator()))
                .createdAt(group.getCreatedAt())
                .build();
    }

    private StudyGroupDetailResponse toDetailResponse(StudyGroup group, StudyGroupRole myRole) {
        List<StudyGroupMemberResponse> members = studyGroupMemberRepository
                .findAllByIdGroupIdOrderByJoinedAtAsc(group.getId())
                .stream()
                .map(m -> StudyGroupMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .fullName(m.getUser().getFullName())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .toList();

        List<StudyGroupSharedSetResponse> sharedSets = studyGroupSetRepository
                .findAllByIdGroupIdOrderBySharedAtDesc(group.getId())
                .stream()
                .filter(s -> !s.getFlashcardSet().isDeleted())
                .map(s -> StudyGroupSharedSetResponse.builder()
                        .set(flashcardSetService.toSummaryResponse(s.getFlashcardSet()))
                        .sharedByUserId(s.getSharedBy().getId())
                        .sharedByUsername(s.getSharedBy().getUsername())
                        .sharedByFullName(s.getSharedBy().getFullName())
                        .sharedAt(s.getSharedAt())
                        .build())
                .toList();

        return StudyGroupDetailResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .inviteCode(group.getInviteCode())
                .myRole(myRole)
                .creator(toOwnerSummary(group.getCreator()))
                .members(members)
                .sharedSets(sharedSets)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private SetSummaryResponse.OwnerSummary toOwnerSummary(User user) {
        return SetSummaryResponse.OwnerSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .verifiedTeacher(user.isVerifiedTeacher())
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
