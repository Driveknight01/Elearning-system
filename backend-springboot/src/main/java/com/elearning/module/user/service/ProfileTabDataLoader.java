package com.elearning.module.user.service;

import com.elearning.module.user.dto.PublicProfileResponse.ProfileAchievementItem;
import com.elearning.module.user.dto.PublicProfileResponse.ProfileClassItem;
import com.elearning.module.user.dto.PublicProfileResponse.ProfileFolderItem;
import com.elearning.module.user.dto.PublicProfileResponse.ProfileSetItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Lightweight SQL reader for loading a user's visible profile tabs (Sets, Folders, Classes, Achievements)
 * directly from PostgreSQL tables before their full domain modules are implemented.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTabDataLoader {

    private final JdbcTemplate jdbcTemplate;

    public List<ProfileSetItem> loadSets(Long userId, boolean includePrivate) {
        String sql = includePrivate
                ? """
                  SELECT id, title, description, visibility, card_count, view_count, avg_rating, created_at
                  FROM flashcard_sets
                  WHERE owner_id = ? AND deleted_at IS NULL
                  ORDER BY created_at DESC
                  LIMIT 50
                  """
                : """
                  SELECT id, title, description, visibility, card_count, view_count, avg_rating, created_at
                  FROM flashcard_sets
                  WHERE owner_id = ? AND visibility = 'PUBLIC' AND moderation_status = 'APPROVED' AND deleted_at IS NULL
                  ORDER BY created_at DESC
                  LIMIT 50
                  """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> ProfileSetItem.builder()
                    .id(rs.getLong("id"))
                    .title(rs.getString("title"))
                    .description(rs.getString("description"))
                    .visibility(rs.getString("visibility"))
                    .cardCount(rs.getInt("card_count"))
                    .viewCount(rs.getInt("view_count"))
                    .avgRating(rs.getBigDecimal("avg_rating") != null ? rs.getBigDecimal("avg_rating").doubleValue() : 0.0)
                    .createdAt(toInstant(rs.getTimestamp("created_at")))
                    .build(), userId);
        } catch (BadSqlGrammarException ex) {
            log.debug("Skipping flashcard_sets profile query (schema not present in current context): {}", ex.getMessage());
            return List.of();
        }
    }

    public List<ProfileFolderItem> loadFolders(Long userId, boolean includePrivate) {
        String sql = includePrivate
                ? """
                  SELECT f.id, f.name, f.description, f.visibility, f.created_at,
                         (SELECT COUNT(*) FROM folder_sets fs WHERE fs.folder_id = f.id) AS set_count
                  FROM folders f
                  WHERE f.owner_id = ?
                  ORDER BY f.created_at DESC
                  LIMIT 50
                  """
                : """
                  SELECT f.id, f.name, f.description, f.visibility, f.created_at,
                         (SELECT COUNT(*) FROM folder_sets fs WHERE fs.folder_id = f.id) AS set_count
                  FROM folders f
                  WHERE f.owner_id = ? AND f.visibility = 'PUBLIC'
                  ORDER BY f.created_at DESC
                  LIMIT 50
                  """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> ProfileFolderItem.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .visibility(rs.getString("visibility"))
                    .setCount(rs.getInt("set_count"))
                    .createdAt(toInstant(rs.getTimestamp("created_at")))
                    .build(), userId);
        } catch (BadSqlGrammarException ex) {
            log.debug("Skipping folders profile query (schema not present in current context): {}", ex.getMessage());
            return List.of();
        }
    }

    public List<ProfileClassItem> loadClasses(Long userId) {
        String sql = """
                SELECT c.id, c.name, c.school_name,
                       CASE WHEN c.teacher_id = ? THEN 'TEACHER' ELSE 'STUDENT' END AS member_role,
                       (SELECT COUNT(*) FROM class_members cm2 WHERE cm2.class_id = c.id AND cm2.status = 'APPROVED') AS member_count
                FROM classes c
                LEFT JOIN class_members cm ON cm.class_id = c.id AND cm.user_id = ? AND cm.status = 'APPROVED'
                WHERE c.deleted_at IS NULL AND (c.teacher_id = ? OR cm.user_id IS NOT NULL)
                ORDER BY c.created_at DESC
                LIMIT 50
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> ProfileClassItem.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .schoolName(rs.getString("school_name"))
                    .memberRole(rs.getString("member_role"))
                    .memberCount(rs.getInt("member_count"))
                    .build(), userId, userId, userId);
        } catch (BadSqlGrammarException ex) {
            log.debug("Skipping classes profile query (schema not present in current context): {}", ex.getMessage());
            return List.of();
        }
    }

    public List<ProfileAchievementItem> loadAchievements(Long userId) {
        String sql = """
                SELECT a.id, a.code, a.name, a.description, a.icon_url, a.xp_reward, ua.unlocked_at
                FROM user_achievements ua
                JOIN achievements a ON a.id = ua.achievement_id
                WHERE ua.user_id = ?
                ORDER BY ua.unlocked_at DESC
                LIMIT 50
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> ProfileAchievementItem.builder()
                    .id(rs.getLong("id"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .iconUrl(rs.getString("icon_url"))
                    .xpReward(rs.getInt("xp_reward"))
                    .unlockedAt(toInstant(rs.getTimestamp("unlocked_at")))
                    .build(), userId);
        } catch (BadSqlGrammarException ex) {
            log.debug("Skipping achievements profile query (schema not present in current context): {}", ex.getMessage());
            return List.of();
        }
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
