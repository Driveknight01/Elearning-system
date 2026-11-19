package com.elearning.smartflash.ui.set;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.elearning.smartflash.databinding.ActivityScanDocumentBinding;
import com.elearning.smartflash.util.ThemeManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan Document screen ("Vuốt để chọn") allowing the user to capture or pick an image,
 * swipe across detected OCR word boxes, and add flashcards back to Create/Edit Set.
 */
public class ScanDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED_TERMS = "extra_scanned_terms";
    public static final String EXTRA_SCANNED_DEFS = "extra_scanned_defs";

    private ActivityScanDocumentBinding binding;
    private final ArrayList<String> scannedTerms = new ArrayList<>();
    private final ArrayList<String> scannedDefs = new ArrayList<>();
    private boolean focusingTerm = true;

    private final ActivityResultLauncher<Void> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    processBitmap(bitmap);
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Bitmap bitmap = loadBitmapFromUri(uri);
                    if (bitmap != null) {
                        processBitmap(bitmap);
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

        setupTextboxes();
        setupButtons();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithScannedCards();
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
        binding.btnClose.setOnClickListener(v -> finishWithScannedCards());

        binding.btnTakePhoto.setOnClickListener(v -> takePhotoLauncher.launch(null));
        binding.btnPickGallery.setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));

        binding.btnChangeImage.setOnClickListener(v -> {
            binding.ocrImageView.setVisibility(View.GONE);
            binding.layoutBottomControls.setVisibility(View.GONE);
            binding.layoutEmptyActions.setVisibility(View.VISIBLE);
        });

        binding.btnDoneScan.setOnClickListener(v -> finishWithScannedCards());

        binding.btnAddCard.setOnClickListener(v -> {
            String term = binding.etTerm.getText() != null ? binding.etTerm.getText().toString().trim() : "";
            String def = binding.etDefinition.getText() != null ? binding.etDefinition.getText().toString().trim() : "";
            if (term.isEmpty() || def.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ Thuật ngữ và Định nghĩa", Toast.LENGTH_SHORT).show();
                return;
            }
            scannedTerms.add(term);
            scannedDefs.add(def);
            binding.etTerm.setText("");
            binding.etDefinition.setText("");
            binding.ocrImageView.clearSelection();
            binding.etTerm.requestFocus();

            binding.btnDoneScan.setVisibility(View.VISIBLE);
            binding.btnDoneScan.setText("Xong (" + scannedTerms.size() + ")");
            Toast.makeText(this, "Đã thêm thẻ #" + scannedTerms.size(), Toast.LENGTH_SHORT).show();
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

    private void processBitmap(Bitmap bitmap) {
        binding.layoutEmptyActions.setVisibility(View.GONE);
        binding.ocrImageView.setVisibility(View.VISIBLE);
        binding.layoutBottomControls.setVisibility(View.VISIBLE);
        binding.progressOcr.setVisibility(View.VISIBLE);
        binding.ocrImageView.setImageAndWords(bitmap, null);

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
                    if (boxes.isEmpty()) {
                        Toast.makeText(this, "Không nhận diện được chữ trong ảnh", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressOcr.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi nhận diện văn bản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void finishWithScannedCards() {
        String term = binding.etTerm.getText() != null ? binding.etTerm.getText().toString().trim() : "";
        String def = binding.etDefinition.getText() != null ? binding.etDefinition.getText().toString().trim() : "";
        if (!term.isEmpty() && !def.isEmpty() && scannedTerms.isEmpty()) {
            scannedTerms.add(term);
            scannedDefs.add(def);
        }

        if (!scannedTerms.isEmpty()) {
            Intent data = new Intent();
            data.putStringArrayListExtra(EXTRA_SCANNED_TERMS, scannedTerms);
            data.putStringArrayListExtra(EXTRA_SCANNED_DEFS, scannedDefs);
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
