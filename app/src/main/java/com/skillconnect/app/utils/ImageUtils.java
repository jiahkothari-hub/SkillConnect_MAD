package com.skillconnect.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Image loading (Glide) and compression before upload. */
public final class ImageUtils {

    private static final int MAX_DIMENSION = 1280;
    private static final int JPEG_QUALITY = 80;

    private ImageUtils() {
    }

    /** Shows a photo, or a coloured initials avatar when there is no photo. */
    public static void loadAvatar(ImageView view, String url, String name) {
        AvatarDrawable fallback = new AvatarDrawable(name);
        if (url == null || url.trim().isEmpty()) {
            Glide.with(view).clear(view);
            view.setImageDrawable(fallback);
            return;
        }
        Glide.with(view).load(url).placeholder(fallback).error(fallback).centerCrop().into(view);
    }

    public static void loadImage(ImageView view, String url) {
        Glide.with(view).load(url).centerCrop().into(view);
    }

    public static void loadImage(ImageView view, Uri uri) {
        Glide.with(view).load(uri).centerCrop().into(view);
    }

    /**
     * Reads an image from a content/file Uri, fixes camera rotation, scales it down to
     * max 1280px and returns JPEG bytes. Call from a background thread.
     */
    public static byte[] compressImage(Context context, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Not an image");

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION || bounds.outHeight / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IOException("Could not decode image");

        int rotation = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (Exception ignored) {
            // No EXIF data – keep rotation 0
        }

        float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if (transformed != bitmap) bitmap.recycle();
            bitmap = transformed;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        bitmap.recycle();
        return out.toByteArray();
    }
}
