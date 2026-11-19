package com.elearning.smartflash.ui.profile;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.ui.profile.AvatarChoiceAdapter.AvatarTile;
import com.elearning.smartflash.util.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Full-screen Quizlet-style "Chọn ảnh đại diện" picker matching Web's 12 preset avatars
 * plus Gallery/Camera actions.
 */
public final class ChooseAvatarDialogHelper {

    public interface Callback {
        void onPickFromGallery();

        void onPresetConfirmed(Uri renderedPngUri);
    }

    private ChooseAvatarDialogHelper() {
    }

    public static void show(Context context, Callback callback) {
        Dialog dialog = new Dialog(context, R.style.Theme_SmartFlash);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_choose_avatar, null);
        dialog.setContentView(view);

        ThemeManager.applyStatusBarColor(dialog.getWindow(), context, R.color.sf_bg_screen);

        ImageButton btnBack = view.findViewById(R.id.btn_avatar_back);
        ImageButton btnConfirm = view.findViewById(R.id.btn_avatar_confirm);
        RecyclerView rvGrid = view.findViewById(R.id.rv_avatar_choices);

        List<AvatarTile> tiles = Arrays.asList(
                AvatarTile.action(AvatarTile.TYPE_CAMERA, R.drawable.ic_camera),
                AvatarTile.action(AvatarTile.TYPE_GALLERY, R.drawable.ic_gallery),
                AvatarTile.preset("owl", "#4255FF", "\uD83E\uDD89"),
                AvatarTile.preset("fox", "#FF8A4C", "\uD83E\uDD8A"),
                AvatarTile.preset("panda", "#10B981", "\uD83D\uDC3C"),
                AvatarTile.preset("penguin", "#38BDF8", "\uD83D\uDC27"),
                AvatarTile.preset("cat", "#A855F7", "\uD83D\uDC31"),
                AvatarTile.preset("koala", "#6366F1", "\uD83D\uDC28"),
                AvatarTile.preset("rabbit", "#EC4899", "\uD83D\uDC30"),
                AvatarTile.preset("bear", "#F59E0B", "\uD83D\uDC3B"),
                AvatarTile.preset("whale", "#0EA5E9", "\uD83D\uDC33"),
                AvatarTile.preset("duck", "#22C55E", "\uD83E\uDD86"),
                AvatarTile.preset("bee", "#EAB308", "\uD83D\uDC1D"),
                AvatarTile.preset("unicorn", "#8B5CF6", "\uD83E\uDD84")
        );

        final AvatarTile[] selectedPreset = {null};
        final AvatarChoiceAdapter[] adapterHolder = new AvatarChoiceAdapter[1];

        adapterHolder[0] = new AvatarChoiceAdapter(tiles, (tile, position) -> {
            if (tile.type == AvatarTile.TYPE_CAMERA || tile.type == AvatarTile.TYPE_GALLERY) {
                dialog.dismiss();
                callback.onPickFromGallery();
                return;
            }
            selectedPreset[0] = tile;
            adapterHolder[0].setSelectedPosition(position);
            btnConfirm.setEnabled(true);
            btnConfirm.setColorFilter(ContextCompat.getColor(context, R.color.quizlet_blue));
        });

        rvGrid.setLayoutManager(new GridLayoutManager(context, 3));
        rvGrid.setAdapter(adapterHolder[0]);

        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (selectedPreset[0] != null) {
                Uri uri = renderPresetToCachePng(context, selectedPreset[0]);
                dialog.dismiss();
                if (uri != null) {
                    callback.onPresetConfirmed(uri);
                }
            }
        });

        dialog.show();
    }

    private static Uri renderPresetToCachePng(Context context, AvatarTile preset) {
        try {
            int size = 256;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.parseColor(preset.bgHex));

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(132f);
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float centerY = size / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(preset.emoji, size / 2f, centerY, paint);

            File outFile = new File(context.getCacheDir(), "preset_avatar_" + preset.id + ".png");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            return Uri.fromFile(outFile);
        } catch (Exception ignored) {
            return null;
        }
    }
}
