package com.elearning.smartflash.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProfileUtilsTest {

    @Test
    public void resolveImageUrl_mapsLocalhostAndRelativePathsToServerOrigin() {
        assertNull(ProfileUtils.resolveImageUrl(null));
        assertNull(ProfileUtils.resolveImageUrl("   "));
        assertEquals(
                "http://10.0.2.2:8080/uploads/avatars/a.png",
                ProfileUtils.resolveImageUrl("/uploads/avatars/a.png")
        );
        assertEquals(
                "http://10.0.2.2:8080/uploads/avatars/b.jpg",
                ProfileUtils.resolveImageUrl("http://localhost:8080/uploads/avatars/b.jpg")
        );
        assertEquals(
                "https://lh3.googleusercontent.com/a/photo.png",
                ProfileUtils.resolveImageUrl("https://lh3.googleusercontent.com/a/photo.png")
        );
    }

    @Test
    public void getInitials_extractsTwoLetters() {
        assertEquals("NA", ProfileUtils.getInitials("Nguyen An", "nguyenvana"));
        assertEquals("ST", ProfileUtils.getInitials("", "student01"));
        assertEquals("SF", ProfileUtils.getInitials(null, null));
    }

    @Test
    public void labels_mapRolesAndVisibilityToVietnamese() {
        assertEquals("Giáo viên", ProfileUtils.getRoleLabel("TEACHER"));
        assertEquals("Học viên", ProfileUtils.getRoleLabel("USER"));
        assertEquals("Công khai", ProfileUtils.getVisibilityLabel("PUBLIC"));
        assertEquals("Chỉ thành viên", ProfileUtils.getVisibilityLabel("LOGGED_IN_ONLY"));
        assertEquals("Riêng tư", ProfileUtils.getVisibilityLabel("PRIVATE"));
    }
}
