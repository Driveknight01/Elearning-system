package com.elearning.smartflash.util;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link TtsHelper#resolveLocale(String)} and {@link TtsHelper#detectEffectiveShortLang(String, String)}.
 */
public class TtsHelperTest {

    @Test
    public void resolveLocale_handlesStandardLanguageCodes() {
        assertEquals(new Locale("vi", "VN"), TtsHelper.resolveLocale("vi"));
        assertEquals(Locale.US, TtsHelper.resolveLocale("en"));
        assertEquals(Locale.JAPAN, TtsHelper.resolveLocale("ja"));
        assertEquals(Locale.KOREA, TtsHelper.resolveLocale("ko"));
        assertEquals(Locale.SIMPLIFIED_CHINESE, TtsHelper.resolveLocale("zh"));
        assertEquals(Locale.FRANCE, TtsHelper.resolveLocale("fr"));
        assertEquals(Locale.GERMANY, TtsHelper.resolveLocale("de"));
    }

    @Test
    public void resolveLocale_handlesRegionalCodesAndFallback() {
        assertEquals(new Locale("en", "GB"), TtsHelper.resolveLocale("en-GB"));
        assertEquals(new Locale("vi", "VN"), TtsHelper.resolveLocale("vi_vn"));
        assertEquals(Locale.US, TtsHelper.resolveLocale(null));
        assertEquals(Locale.US, TtsHelper.resolveLocale("   "));
    }

    @Test
    public void detectEffectiveShortLang_switchesPureAsciiDefinitionFromViToEn() {
        assertEquals("en", TtsHelper.detectEffectiveShortLang("accelerator", "vi"));
        assertEquals("en", TtsHelper.detectEffectiveShortLang("to make something happen faster", "vi"));
        assertEquals("vi", TtsHelper.detectEffectiveShortLang("bộ tăng tốc", "vi"));
        assertEquals("vi", TtsHelper.detectEffectiveShortLang("tăng tốc (accelerate)", "vi"));
        assertEquals("fr", TtsHelper.detectEffectiveShortLang("bonjour", "fr"));
    }
}
