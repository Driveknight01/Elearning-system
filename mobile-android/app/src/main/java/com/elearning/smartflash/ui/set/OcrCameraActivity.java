package com.elearning.smartflash.ui.set;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.elearning.smartflash.databinding.ActivityOcrCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * CameraX document capture screen returning a high-resolution JPEG file path for OCR extraction.
 */
public class OcrCameraActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    private ActivityOcrCameraBinding binding;
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean flashEnabled = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Cần quyền Camera để chụp ảnh tài liệu", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOcrCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnToggleFlash.setOnClickListener(v -> toggleFlash());
        binding.btnCapture.setOnClickListener(v -> capturePhoto());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Không thể khởi động CameraX: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Thiết bị không hỗ trợ đèn Flash", Toast.LENGTH_SHORT).show();
            return;
        }
        flashEnabled = !flashEnabled;
        camera.getCameraControl().enableTorch(flashEnabled);
        binding.btnToggleFlash.setText(flashEnabled ? "Flash: Bật" : "Flash: Tắt");
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            return;
        }
        binding.btnCapture.setEnabled(false);
        binding.progressCapture.setVisibility(View.VISIBLE);

        File outputFile = new File(getCacheDir(), "ocr_capture_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        binding.progressCapture.setVisibility(View.GONE);
                        Intent data = new Intent();
                        data.putExtra(EXTRA_IMAGE_PATH, outputFile.getAbsolutePath());
                        setResult(RESULT_OK, data);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        binding.progressCapture.setVisibility(View.GONE);
                        binding.btnCapture.setEnabled(true);
                        Toast.makeText(OcrCameraActivity.this,
                                "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
