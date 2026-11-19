package com.elearning.smartflash.ui.search;

/**
 * UI models for Search results tabs (Học phần, Câu hỏi, Người dùng, Lớp học).
 */
public final class SearchResultModels {

    private SearchResultModels() {
    }

    public static class SetItem {
        public final long id;
        public final String title;
        public final int termCount;
        public final String author;
        public final String avatarHex;
        public final boolean hasImage;
        public final boolean hasDiagram;
        public final String creatorType; // "all", "plus", "teacher", "verified"

        public SetItem(String title, int termCount, String author, String avatarHex,
                       boolean hasImage, boolean hasDiagram, String creatorType) {
            this(-1L, title, termCount, author, avatarHex, hasImage, hasDiagram, creatorType);
        }

        public SetItem(long id, String title, int termCount, String author, String avatarHex,
                       boolean hasImage, boolean hasDiagram, String creatorType) {
            this.id = id;
            this.title = title;
            this.termCount = termCount;
            this.author = author;
            this.avatarHex = avatarHex;
            this.hasImage = hasImage;
            this.hasDiagram = hasDiagram;
            this.creatorType = creatorType;
        }
    }

    public static class QuestionItem {
        public final String subject;
        public final String body;

        public QuestionItem(String subject, String body) {
            this.subject = subject;
            this.body = body;
        }
    }

    public static class UserItem {
        public final String username;
        public final int setCount;
        public final int classCount;
        public final String avatarHex;

        public UserItem(String username, int setCount, int classCount, String avatarHex) {
            this.username = username;
            this.setCount = setCount;
            this.classCount = classCount;
            this.avatarHex = avatarHex;
        }
    }

    public static class ClassItem {
        public final String title;
        public final String schoolInfo;
        public final int setCount;
        public final int memberCount;

        public ClassItem(String title, String schoolInfo, int setCount, int memberCount) {
            this.title = title;
            this.schoolInfo = schoolInfo;
            this.setCount = setCount;
            this.memberCount = memberCount;
        }
    }
}
