package com.skillconnect.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.models.PortfolioItem;

/** Shows a portfolio image larger, with its description. */
public final class ImagePreviewDialog {

    private ImagePreviewDialog() {
    }

    public static void show(Context context, PortfolioItem item, int placeholderIcon) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null);
        ImageView image = view.findViewById(R.id.ivPreview);
        TextView caption = view.findViewById(R.id.tvPreviewCaption);
        if (item.isDemoPlaceholder()) {
            image.setScaleType(ImageView.ScaleType.CENTER);
            image.setImageResource(placeholderIcon);
        } else {
            ImageUtils.loadImage(image, item.getImageUrl());
        }
        String description = item.getDescription();
        caption.setText(description != null && !description.isEmpty() ? description : "Portfolio image");
        new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(R.string.close, null)
                .show();
    }
}
