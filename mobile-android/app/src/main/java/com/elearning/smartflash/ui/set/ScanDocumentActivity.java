package com.elearning.smartflash.ui.set;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.OcrExtractResponseDto;
import com.elearning.smartflash.data.remote.dto.OcrExtractedCardDto;
import com.elearning.smartflash.data.remote.dto.OcrTextBoxDto;
import com.elearning.smartflash.data.repository.FlashcardSetRepository;
import com.elearning.smartflash.databinding.ActivityScanDocumentBinding;
import com.elearning.smartflash.util.ThemeManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scan Document screen ("Vuốt để chọn") integrating CameraX capture, backend OCR extraction
 * with offline ML Kit fallback, global column + separator parsing, and live auto-import to CRUD.
 */
public class ScanDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED_TERMS = "extra_scanned_terms";
    public static final String EXTRA_SCANNED_DEFS = "extra_scanned_defs";
    public static final String EXTRA_TERM_LANG = "extra_term_lang";
    public static final String EXTRA_DEF_LANG = "extra_def_lang";
    public static final String EXTRA_LIVE_RECEIVER = "extra_live_receiver";

    private ActivityScanDocumentBinding binding;
    private FlashcardSetRepository repository;
    private final ArrayList<String> manualTerms = new ArrayList<>();
    private final ArrayList<String> manualDefs = new ArrayList<>();
    private final List<OcrExtractedCardDto> autoExtractedCards = new ArrayList<>();
    private boolean focusingTerm = true;
    private boolean autoImportMode = false;
    private String sourceLang;
    private String targetLang;
    private ResultReceiver liveReceiver;

    private final ActivityResultLauncher<Intent> cameraXLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String imagePath = result.getData().getStringExtra(OcrCameraActivity.EXTRA_IMAGE_PATH);
                if (imagePath != null) {
                    File file = new File(imagePath);
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null && file.exists()) {
                        processImage(bitmap, file);
                    } else {
                        Toast.makeText(this, "Không thể đọc ảnh từ Camera", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Bitmap bitmap = loadBitmapFromUri(uri);
                    if (bitmap != null) {
                        File file = saveBitmapToCache(bitmap);
                        processImage(bitmap, file);
                    } else {
                        Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();
        binding = ActivityScanDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new FlashcardSetRepository(this);
        sourceLang = getIntent().getStringExtra(EXTRA_TERM_LANG);
        targetLang = getIntent().getStringExtra(EXTRA_DEF_LANG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            liveReceiver = getIntent().getParcelableExtra(EXTRA_LIVE_RECEIVER, ResultReceiver.class);
        } else {
            liveReceiver = getIntent().getParcelableExtra(EXTRA_LIVE_RECEIVER);
        }

        setupTextboxes();
        setupButtons();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithManualCards();
            }
        });
    }

    private void applyStatusBar() {
        Window window = getWindow();
        if (window == null) return;
        boolean nightMode = ThemeManager.isNightMode(this);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_surface));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.quizlet_bg));
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(!nightMode);
    }

    private void setupTextboxes() {
        binding.etTerm.setOnFocusChangeListener((v, hasFocus) -> {
            updateUnderline(binding.viewTermUnderline, hasFocus);
            if (hasFocus) {
                focusingTerm = true;
            }
        });
        binding.etDefinition.setOnFocusChangeListener((v, hasFocus) -> {
            updateUnderline(binding.viewDefUnderline, hasFocus);
            if (hasFocus) {
                focusingTerm = false;
            }
        });
        binding.etTerm.requestFocus();
    }

    private void setupButtons() {
        binding.btnClose.setOnClickListener(v -> finishWithManualCards());

        binding.btnTakePhoto.setOnClickListener(v ->
                cameraXLauncher.launch(new Intent(this, OcrCameraActivity.class)));

        binding.btnPickGallery.setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));

        binding.btnChangeImage.setOnClickListener(v -> {
            binding.ocrImageView.setVisibility(View.GONE);
            binding.layoutBottomControls.setVisibility(View.GONE);
            binding.layoutEmptyActions.setVisibility(View.VISIBLE);
            resetToSaveMode();
        });

        binding.btnDoneScan.setOnClickListener(v -> {
            if (autoImportMode && !autoExtractedCards.isEmpty()) {
                dispatchAutoCardsToCrudAndStay();
            } else {
                finishWithManualCards();
            }
        });

        binding.btnAddCard.setOnClickListener(v -> {
            String term = binding.etTerm.getText() != null ? binding.etTerm.getText().toString().trim() : "";
            String def = binding.etDefinition.getText() != null ? binding.etDefinition.getText().toString().trim() : "";
            if (term.isEmpty() || def.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ Thuật ngữ và Định nghĩa", Toast.LENGTH_SHORT).show();
                return;
            }
            manualTerms.add(term);
            manualDefs.add(def);
            binding.etTerm.setText("");
            binding.etDefinition.setText("");
            binding.ocrImageView.clearSelection();
            binding.etTerm.requestFocus();

            if (!autoImportMode) {
                updateSaveButtonLabel();
            }
            Toast.makeText(this, "Đã thêm thẻ #" + manualTerms.size(), Toast.LENGTH_SHORT).show();
        });

        binding.ocrImageView.setOnTextSelectedListener(selectedText -> {
            if (selectedText.isEmpty()) return;
            if (focusingTerm) {
                binding.etTerm.setText(selectedText);
                binding.etTerm.setSelection(selectedText.length());
                binding.etDefinition.requestFocus();
            } else {
                binding.etDefinition.setText(selectedText);
                binding.etDefinition.setSelection(selectedText.length());
            }
        });
    }

    private void dispatchAutoCardsToCrudAndStay() {
        ArrayList<String> terms = new ArrayList<>();
        ArrayList<String> defs = new ArrayList<>();
        for (OcrExtractedCardDto card : autoExtractedCards) {
            if (card.getTerm() != null && card.getDefinition() != null) {
                terms.add(card.getTerm());
                defs.add(card.getDefinition());
            }
        }
        if (terms.isEmpty()) {
            resetToSaveMode();
            return;
        }
        if (liveReceiver != null) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(EXTRA_SCANNED_TERMS, terms);
            bundle.putStringArrayList(EXTRA_SCANNED_DEFS, defs);
            liveReceiver.send(RESULT_OK, bundle);
        } else {
            // Fallback if launched without live receiver
            manualTerms.addAll(terms);
            manualDefs.addAll(defs);
        }
        int importedCount = terms.size();
        autoExtractedCards.clear();
        resetToSaveMode();
        Toast.makeText(this,
                "Đã nhập " + importedCount + " thẻ vào bộ thẻ! Bạn có thể vuốt chọn thêm.",
                Toast.LENGTH_LONG).show();
    }

    private void switchToAutoImportMode(List<OcrExtractedCardDto> cards) {
        if (cards == null || cards.isEmpty()) return;
        autoExtractedCards.clear();
        autoExtractedCards.addAll(cards);
        autoImportMode = true;
        binding.btnDoneScan.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_green)));
        binding.btnDoneScan.setText("Nhập tự động (" + autoExtractedCards.size() + ")");
    }

    private void resetToSaveMode() {
        autoImportMode = false;
        binding.btnDoneScan.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_blue)));
        updateSaveButtonLabel();
    }

    private void updateSaveButtonLabel() {
        if (manualTerms.isEmpty()) {
            binding.btnDoneScan.setText("Lưu thẻ");
        } else {
            binding.btnDoneScan.setText("Lưu thẻ (" + manualTerms.size() + ")");
        }
    }

    private void processImage(Bitmap bitmap, File imageFile) {
        binding.layoutEmptyActions.setVisibility(View.GONE);
        binding.ocrImageView.setVisibility(View.VISIBLE);
        binding.layoutBottomControls.setVisibility(View.VISIBLE);
        resetToSaveMode();
        binding.progressOcr.setVisibility(View.VISIBLE);
        binding.ocrImageView.setImageAndWords(bitmap, null);
        autoExtractedCards.clear();

        if (imageFile == null || !imageFile.exists()) {
            runMlKitFallback(bitmap);
            return;
        }

        repository.extractOcrCards(imageFile, sourceLang, targetLang,
                new FlashcardSetRepository.ResultCallback<>() {
                    @Override
                    public void onSuccess(OcrExtractResponseDto data) {
                        if (isFinishing() || isDestroyed()) return;
                        binding.progressOcr.setVisibility(View.GONE);

                        if (data != null && data.getCards() != null && !data.getCards().isEmpty()) {
                            switchToAutoImportMode(data.getCards());
                        } else if (data != null && data.getRawText() != null && !data.getRawText().trim().isEmpty()) {
                            List<OcrExtractedCardDto> parsed = parseSeparatorCardsFromLines(
                                    Arrays.asList(data.getRawText().split("\\r?\\n")));
                            if (!parsed.isEmpty()) {
                                switchToAutoImportMode(parsed);
                            }
                        }

                        List<OcrHighlightImageView.WordBox> boxes = convertNormalizedBoxes(
                                data != null ? data.getBoxes() : null,
                                bitmap.getWidth(),
                                bitmap.getHeight()
                        );
                        if (!boxes.isEmpty()) {
                            if (autoExtractedCards.isEmpty()) {
                                List<OcrExtractedCardDto> rowCards =
                                        extractCardsFromWordBoxes(boxes, bitmap.getWidth());
                                if (!rowCards.isEmpty()) {
                                    switchToAutoImportMode(rowCards);
                                }
                            }
                            binding.ocrImageView.setImageAndWords(bitmap, boxes);
                        } else {
                            runMlKitFallback(bitmap);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        runMlKitFallback(bitmap);
                    }
                });
    }

    private List<OcrHighlightImageView.WordBox> convertNormalizedBoxes(
            List<OcrTextBoxDto> dtoBoxes, int imgWidth, int imgHeight) {
        List<OcrHighlightImageView.WordBox> result = new ArrayList<>();
        if (dtoBoxes == null || imgWidth <= 0 || imgHeight <= 0) {
            return result;
        }
        for (OcrTextBoxDto dto : dtoBoxes) {
            if (dto.getText() == null || dto.getText().trim().isEmpty()) continue;
            if (dto.getX() == null || dto.getY() == null
                    || dto.getWidth() == null || dto.getHeight() == null) {
                continue;
            }
            int left = Math.max(0, (int) Math.round(dto.getX() * imgWidth));
            int top = Math.max(0, (int) Math.round(dto.getY() * imgHeight));
            int right = Math.min(imgWidth, (int) Math.round((dto.getX() + dto.getWidth()) * imgWidth));
            int bottom = Math.min(imgHeight, (int) Math.round((dto.getY() + dto.getHeight()) * imgHeight));
            if (right > left && bottom > top) {
                result.add(new OcrHighlightImageView.WordBox(
                        dto.getText().trim(), new Rect(left, top, right, bottom)));
            }
        }
        return result;
    }

    private void runMlKitFallback(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    binding.progressOcr.setVisibility(View.GONE);
                    List<OcrHighlightImageView.WordBox> boxes = new ArrayList<>();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                                if (element.getBoundingBox() != null) {
                                    boxes.add(new OcrHighlightImageView.WordBox(
                                            element.getText(), element.getBoundingBox()));
                                }
                            }
                        }
                    }
                    binding.ocrImageView.setImageAndWords(bitmap, boxes);
                    if (autoExtractedCards.isEmpty() && !boxes.isEmpty()) {
                        List<OcrExtractedCardDto> fallbackCards =
                                extractCardsFromWordBoxes(boxes, bitmap.getWidth());
                        if (!fallbackCards.isEmpty()) {
                            switchToAutoImportMode(fallbackCards);
                        }
                    }
                    if (boxes.isEmpty()) {
                        Toast.makeText(this, "Không nhận diện được chữ trong ảnh", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressOcr.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi nhận diện văn bản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private List<OcrExtractedCardDto> extractCardsFromWordBoxes(
            List<OcrHighlightImageView.WordBox> boxes, int imgWidth) {
        if (boxes == null || boxes.isEmpty() || imgWidth <= 0) {
            return Collections.emptyList();
        }
        List<List<OcrHighlightImageView.WordBox>> rows = groupBoxesIntoRows(boxes);
        for (List<OcrHighlightImageView.WordBox> row : rows) {
            Collections.sort(row, Comparator.comparingInt(b -> b.bitmapRect.left));
        }

        // 1. Try explicit separator characters across merged row lines
        List<String> rowLines = new ArrayList<>();
        for (List<OcrHighlightImageView.WordBox> row : rows) {
            StringBuilder sb = new StringBuilder();
            for (OcrHighlightImageView.WordBox box : row) {
                String w = box.text != null ? box.text.trim() : "";
                if (w.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(w);
            }
            if (sb.length() > 0) {
                rowLines.add(sb.toString());
            }
        }
        List<OcrExtractedCardDto> separatorCards = parseSeparatorCardsFromLines(rowLines);
        if (!separatorCards.isEmpty()) {
            return separatorCards;
        }

        // 2. Global column alignment + multi-line wrapping + adaptive per-row fallback
        return extractTwoColumnCardsFromRows(rows, imgWidth);
    }

    private List<OcrExtractedCardDto> parseSeparatorCardsFromLines(List<String> lines) {
        List<OcrExtractedCardDto> cards = new ArrayList<>();
        if (lines == null) return cards;
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("^(.+?)\\t+(.+)$"),
                Pattern.compile("^(.+?)\\s+[—–-]\\s+(.+)$"),
                Pattern.compile("^(.+?)\\s*:(?!//)\\s*(.+)$"),
                Pattern.compile("^(.+?)\\s*=\\s*(.+)$"),
                Pattern.compile("^(.+?)\\s*->\\s*(.+)$")
        );
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.length() < 3) continue;
            for (Pattern p : patterns) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String term = m.group(1) != null ? m.group(1).replaceAll("^[\\s\\-:]+|[\\s\\-:]+$", "") : "";
                    String def = m.group(2) != null ? m.group(2).replaceAll("^[\\s\\-:]+|[\\s\\-:]+$", "") : "";
                    if (!term.isEmpty() && !def.isEmpty() && seen.add(term.toLowerCase(Locale.ROOT))) {
                        cards.add(new OcrExtractedCardDto(term, def));
                        break;
                    }
                }
            }
        }
        return cards;
    }

    private List<List<OcrHighlightImageView.WordBox>> groupBoxesIntoRows(
            List<OcrHighlightImageView.WordBox> boxes) {
        List<OcrHighlightImageView.WordBox> sorted = new ArrayList<>(boxes);
        Collections.sort(sorted, Comparator
                .comparingInt((OcrHighlightImageView.WordBox b) -> b.bitmapRect.centerY())
                .thenComparingInt(b -> b.bitmapRect.left));

        List<List<OcrHighlightImageView.WordBox>> rows = new ArrayList<>();
        for (OcrHighlightImageView.WordBox box : sorted) {
            int cy = box.bitmapRect.centerY();
            boolean placed = false;
            for (List<OcrHighlightImageView.WordBox> row : rows) {
                double avgCy = 0;
                double avgH = 0;
                for (OcrHighlightImageView.WordBox item : row) {
                    avgCy += item.bitmapRect.centerY();
                    avgH += item.bitmapRect.height();
                }
                avgCy /= row.size();
                avgH /= row.size();
                if (Math.abs(cy - avgCy) <= Math.max(avgH * 0.65, 10.0)) {
                    row.add(box);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<OcrHighlightImageView.WordBox> newRow = new ArrayList<>();
                newRow.add(box);
                rows.add(newRow);
            }
        }
        return rows;
    }

    private List<OcrExtractedCardDto> extractTwoColumnCardsFromRows(
            List<List<OcrHighlightImageView.WordBox>> rows, int imgWidth) {
        // Detect global vertical split X across all rows (handles long row + short row mix)
        List<Double> candidateSplits = new ArrayList<>();
        for (List<OcrHighlightImageView.WordBox> row : rows) {
            if (row.size() < 2) continue;
            double avgH = 0;
            for (OcrHighlightImageView.WordBox item : row) {
                avgH += item.bitmapRect.height();
            }
            avgH /= row.size();

            List<Integer> positiveGaps = new ArrayList<>();
            int[] gaps = new int[row.size() - 1];
            for (int i = 0; i < row.size() - 1; i++) {
                gaps[i] = row.get(i + 1).bitmapRect.left - row.get(i).bitmapRect.right;
                if (gaps[i] > 0) positiveGaps.add(gaps[i]);
            }
            Collections.sort(positiveGaps);
            double medianSpace = positiveGaps.isEmpty()
                    ? avgH * 0.25
                    : positiveGaps.get(positiveGaps.size() / 2);
            double minColGap = Math.max(Math.max(avgH * 0.9, medianSpace * 2.1), imgWidth * 0.035);

            for (int i = 0; i < gaps.length; i++) {
                if (gaps[i] >= minColGap) {
                    double mid = (row.get(i).bitmapRect.right + row.get(i + 1).bitmapRect.left) / 2.0;
                    candidateSplits.add(mid);
                }
            }
        }

        Double globalSplitX = null;
        if (candidateSplits.size() >= 2) {
            double clusterTol = imgWidth * 0.08;
            List<Double> bestCluster = Collections.emptyList();
            for (double mid : candidateSplits) {
                List<Double> cluster = new ArrayList<>();
                for (double other : candidateSplits) {
                    if (Math.abs(other - mid) <= clusterTol) {
                        cluster.add(other);
                    }
                }
                if (cluster.size() > bestCluster.size()) {
                    bestCluster = cluster;
                }
            }
            if (bestCluster.size() >= 2) {
                double sum = 0;
                for (double v : bestCluster) sum += v;
                globalSplitX = sum / bestCluster.size();
            }
        }

        List<OcrExtractedCardDto> cards = new ArrayList<>();
        Set<String> seenTerms = new LinkedHashSet<>();

        for (List<OcrHighlightImageView.WordBox> row : rows) {
            StringBuilder left = new StringBuilder();
            StringBuilder right = new StringBuilder();

            if (globalSplitX != null) {
                for (OcrHighlightImageView.WordBox box : row) {
                    String word = box.text != null ? box.text.trim() : "";
                    if (word.isEmpty()) continue;
                    if (box.bitmapRect.centerX() < globalSplitX) {
                        if (left.length() > 0) left.append(' ');
                        left.append(word);
                    } else {
                        if (right.length() > 0) right.append(' ');
                        right.append(word);
                    }
                }
                // Wrapped continuation on the right column
                if (left.length() == 0 && right.length() > 0 && !cards.isEmpty()) {
                    OcrExtractedCardDto last = cards.get(cards.size() - 1);
                    cards.set(cards.size() - 1, new OcrExtractedCardDto(
                            last.getTerm(),
                            (last.getDefinition() + " " + right.toString().trim()).trim()
                    ));
                    continue;
                }
            } else {
                if (row.size() < 2) continue;
                double avgH = 0;
                for (OcrHighlightImageView.WordBox item : row) {
                    avgH += item.bitmapRect.height();
                }
                avgH /= row.size();

                List<Integer> positiveGaps = new ArrayList<>();
                int maxGap = -1;
                int splitIdx = -1;
                for (int i = 0; i < row.size() - 1; i++) {
                    int gap = row.get(i + 1).bitmapRect.left - row.get(i).bitmapRect.right;
                    if (gap > 0) positiveGaps.add(gap);
                    if (gap > maxGap) {
                        maxGap = gap;
                        splitIdx = i;
                    }
                }
                Collections.sort(positiveGaps);
                double medianSpace = positiveGaps.isEmpty()
                        ? avgH * 0.25
                        : positiveGaps.get(positiveGaps.size() / 2);
                double minGap = Math.max(Math.max(avgH * 1.1, medianSpace * 2.2), imgWidth * 0.035);

                if (splitIdx >= 0 && maxGap >= minGap) {
                    for (int i = 0; i < row.size(); i++) {
                        String word = row.get(i).text != null ? row.get(i).text.trim() : "";
                        if (word.isEmpty()) continue;
                        if (i <= splitIdx) {
                            if (left.length() > 0) left.append(' ');
                            left.append(word);
                        } else {
                            if (right.length() > 0) right.append(' ');
                            right.append(word);
                        }
                    }
                }
            }

            String term = left.toString().replaceAll("^[\\s\\-:]+|[\\s\\-:]+$", "");
            String def = right.toString().replaceAll("^[\\s\\-:]+|[\\s\\-:]+$", "");
            if (!term.isEmpty() && !def.isEmpty() && seenTerms.add(term.toLowerCase(Locale.ROOT))) {
                cards.add(new OcrExtractedCardDto(term, def));
            }
        }
        return cards;
    }

    private File saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "ocr_upload_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source, (decoder, info, src) ->
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
            } else {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void finishWithManualCards() {
        String term = binding.etTerm.getText() != null ? binding.etTerm.getText().toString().trim() : "";
        String def = binding.etDefinition.getText() != null ? binding.etDefinition.getText().toString().trim() : "";
        if (!term.isEmpty() && !def.isEmpty()) {
            manualTerms.add(term);
            manualDefs.add(def);
        }

        if (!manualTerms.isEmpty()) {
            Intent data = new Intent();
            data.putStringArrayListExtra(EXTRA_SCANNED_TERMS, manualTerms);
            data.putStringArrayListExtra(EXTRA_SCANNED_DEFS, manualDefs);
            setResult(RESULT_OK, data);
        }
        finish();
    }

    private void updateUnderline(View underline, boolean focused) {
        ViewGroup.LayoutParams lp = underline.getLayoutParams();
        float dp = focused ? 3.5f : 1.5f;
        lp.height = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
        underline.setLayoutParams(lp);
        underline.setBackgroundColor(ContextCompat.getColor(
                this, focused ? R.color.sf_underline_focus : R.color.sf_underline_idle));
    }
}
