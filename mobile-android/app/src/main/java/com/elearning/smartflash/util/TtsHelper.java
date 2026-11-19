package com.elearning.smartflash.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Pronunciation helper that prioritizes natural human audio (Free Dictionary API for English words)
 * and Google TTS MP3 streams with local disk caching, falling back to Android {@link TextToSpeech} offline.
 */
public class TtsHelper {

    public interface OnInitListener {
        void onReady(boolean available);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger playbackToken = new AtomicInteger(0);
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build();

    private Context appContext;
    private File cacheDir;
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private boolean initialized = false;
    private boolean available = false;

    public void init(Context context, OnInitListener listener) {
        if (context == null) {
            if (listener != null) listener.onReady(false);
            return;
        }
        this.appContext = context.getApplicationContext();
        this.cacheDir = new File(appContext.getCacheDir(), "tts_cache");
        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }
        textToSpeech = new TextToSpeech(appContext, status -> {
            initialized = true;
            available = (status == TextToSpeech.SUCCESS);
            if (available && textToSpeech != null) {
                textToSpeech.setSpeechRate(1.0f);
                textToSpeech.setPitch(1.0f);
            }
            if (listener != null) {
                listener.onReady(available);
            }
        });
    }

    public boolean isAvailable() {
        return appContext != null;
    }

    public boolean speak(String text, String langCode) {
        if (TextUtils.isEmpty(text) || appContext == null) {
            return false;
        }
        final String cleanText = text.trim();
        final String shortLang = detectEffectiveShortLang(cleanText, langCode);
        final int token = playbackToken.incrementAndGet();
        stopActivePlayersOnly();

        executor.execute(() -> {
            if (playbackToken.get() != token) return;
            try {
                File cachedFile = getCacheFile(shortLang, cleanText);
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 128) {
                    playAudioFile(cachedFile, token);
                    return;
                }

                byte[] audioBytes = null;
                if ("en".equals(shortLang) && !cleanText.contains(" ") && cleanText.length() <= 40) {
                    audioBytes = fetchDictionaryAudio(cleanText);
                }
                if (playbackToken.get() != token) return;

                if (audioBytes == null || audioBytes.length <= 128) {
                    audioBytes = fetchGoogleTtsAudio(cleanText, shortLang);
                }
                if (playbackToken.get() != token) return;

                if (audioBytes != null && audioBytes.length > 128 && cachedFile != null) {
                    try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
                        fos.write(audioBytes);
                    }
                    playAudioFile(cachedFile, token);
                    return;
                }
            } catch (Exception ignored) {
                // Fall back to system TTS below
            }

            if (playbackToken.get() == token) {
                mainHandler.post(() -> {
                    if (playbackToken.get() == token) {
                        speakWithSystemTts(cleanText, langCode);
                    }
                });
            }
        });
        return true;
    }

    private byte[] fetchDictionaryAudio(String word) {
        try {
            String encoded = URLEncoder.encode(word.toLowerCase(Locale.ROOT), "UTF-8");
            Request req = new Request.Builder()
                    .url("https://api.dictionaryapi.dev/api/v2/entries/en/" + encoded)
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                JSONArray root = new JSONArray(resp.body().string());
                String fallbackAudioUrl = null;
                for (int i = 0; i < root.length(); i++) {
                    JSONObject entry = root.optJSONObject(i);
                    if (entry == null) continue;
                    JSONArray phonetics = entry.optJSONArray("phonetics");
                    if (phonetics == null) continue;
                    for (int j = 0; j < phonetics.length(); j++) {
                        JSONObject p = phonetics.optJSONObject(j);
                        if (p == null) continue;
                        String audioUrl = p.optString("audio", "").trim();
                        if (audioUrl.isEmpty()) continue;
                        if (audioUrl.startsWith("//")) {
                            audioUrl = "https:" + audioUrl;
                        }
                        if (audioUrl.contains("-us.") || audioUrl.contains("_us_")) {
                            return downloadBytes(audioUrl);
                        }
                        if (fallbackAudioUrl == null) {
                            fallbackAudioUrl = audioUrl;
                        }
                    }
                }
                if (fallbackAudioUrl != null) {
                    return downloadBytes(fallbackAudioUrl);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private byte[] fetchGoogleTtsAudio(String text, String shortLang) {
        try {
            String encoded = URLEncoder.encode(text, "UTF-8");
            String url = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl="
                    + URLEncoder.encode(shortLang, "UTF-8") + "&q=" + encoded;
            return downloadBytes(url);
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] downloadBytes(String url) {
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    return resp.body().bytes();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void playAudioFile(File file, int token) {
        mainHandler.post(() -> {
            if (playbackToken.get() != token) return;
            try {
                releaseMediaPlayer();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.setOnCompletionListener(mp -> releaseMediaPlayer());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    releaseMediaPlayer();
                    return true;
                });
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    if (playbackToken.get() == token) {
                        mp.start();
                    } else {
                        releaseMediaPlayer();
                    }
                });
            } catch (Exception ignored) {
                releaseMediaPlayer();
            }
        });
    }

    private void speakWithSystemTts(String text, String langCode) {
        if (!initialized || !available || textToSpeech == null) {
            return;
        }
        String effectiveShort = detectEffectiveShortLang(text, langCode);
        Locale targetLocale = resolveLocale(effectiveShort);
        int langSupport = textToSpeech.isLanguageAvailable(targetLocale);
        if (langSupport == TextToSpeech.LANG_MISSING_DATA || langSupport == TextToSpeech.LANG_NOT_SUPPORTED) {
            Locale fallback = Locale.US;
            int usSupport = textToSpeech.isLanguageAvailable(fallback);
            if (usSupport != TextToSpeech.LANG_MISSING_DATA && usSupport != TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(fallback);
            } else {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        } else {
            textToSpeech.setLanguage(targetLocale);
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sf_tts_" + System.currentTimeMillis());
    }

    private File getCacheFile(String lang, String text) {
        if (cacheDir == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((lang + "|" + text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(String.format(Locale.ROOT, "%02x", hash[i]));
            }
            return new File(cacheDir, sb + ".mp3");
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveShortLangCode(String langCode) {
        Locale locale = resolveLocale(langCode);
        String lang = locale.getLanguage();
        return TextUtils.isEmpty(lang) ? "en" : lang;
    }

    /**
     * Detects whether {@code text} is actually English (pure ASCII Latin without Vietnamese diacritics)
     * when {@code langCode} defaults to Vietnamese, so English definitions use English pronunciation.
     */
    public static String detectEffectiveShortLang(String text, String langCode) {
        String configured = resolveLocale(langCode).getLanguage();
        if (configured == null || configured.isEmpty()) {
            configured = "en";
        }
        if (text == null || text.isEmpty()) {
            return configured;
        }
        boolean hasAsciiLetter = false;
        boolean hasNonAsciiLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                hasAsciiLetter = true;
            } else if (c > 127 && Character.isLetter(c)) {
                hasNonAsciiLetter = true;
                break;
            }
        }
        if ("vi".equals(configured) && hasAsciiLetter && !hasNonAsciiLetter) {
            return "en";
        }
        return configured;
    }

    private void stopActivePlayersOnly() {
        mainHandler.post(() -> {
            releaseMediaPlayer();
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
        });
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    public void stop() {
        playbackToken.incrementAndGet();
        stopActivePlayersOnly();
    }

    public void shutdown() {
        playbackToken.incrementAndGet();
        mainHandler.post(() -> {
            releaseMediaPlayer();
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
            }
        });
        initialized = false;
        available = false;
        executor.shutdownNow();
    }

    /**
     * Resolves a SmartFlash language code (e.g. {@code "vi"}, {@code "en"}, {@code "ja"}) to a {@link Locale}.
     */
    public static Locale resolveLocale(String langCode) {
        if (langCode == null || langCode.trim().isEmpty()) {
            return Locale.US;
        }
        String normalized = langCode.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("-") || normalized.contains("_")) {
            String[] parts = normalized.split("[-_]");
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                return new Locale(parts[0], parts[1].toUpperCase(Locale.ROOT));
            }
            normalized = parts[0];
        }
        switch (normalized) {
            case "vi":
                return new Locale("vi", "VN");
            case "en":
                return Locale.US;
            case "ja":
                return Locale.JAPAN;
            case "ko":
                return Locale.KOREA;
            case "zh":
                return Locale.SIMPLIFIED_CHINESE;
            case "fr":
                return Locale.FRANCE;
            case "de":
                return Locale.GERMANY;
            case "es":
                return new Locale("es", "ES");
            default:
                return new Locale(normalized);
        }
    }
}
