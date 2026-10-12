package com.skillconnect.app.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.Provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * FILE HANDLING: creates .csv and .txt files in the app's private storage, reads them
 * back for a preview and shares them with an implicit ACTION_SEND intent (via FileProvider).
 */
public final class FileUtils {

    public static final String MIME_CSV = "text/csv";
    public static final String MIME_TXT = "text/plain";

    private FileUtils() {
    }

    // ------------------------------------------------------------ writing / reading

    public static File writeTextFile(Context context, String fileName, String content) throws IOException {
        File dir = new File(context.getFilesDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create exports folder");
        File file = new File(dir, fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        return file;
    }

    public static String readTextFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ content builders

    /** booking_history.csv: Service,Provider,Customer,Date,Time,Price (INR),Status */
    public static String buildBookingsCsv(List<Booking> bookings) {
        List<Booking> sorted = new ArrayList<>(bookings);
        Collections.sort(sorted, (a, b) -> Long.compare(b.getScheduledAt(), a.getScheduledAt()));
        StringBuilder sb = new StringBuilder("Service,Provider,Customer,Date,Time,Price (INR),Status\n");
        for (Booking b : sorted) {
            sb.append(csv(b.getService())).append(',')
                    .append(csv(b.getProviderName())).append(',')
                    .append(csv(b.getCustomerName())).append(',')
                    .append(csv(b.getDate())).append(',')
                    .append(csv(b.getTime())).append(',')
                    .append(String.format(Locale.US, "%.0f", b.getPrice())).append(',')
                    .append(csv(b.getStatus())).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** provider_profile.txt */
    public static String buildProviderProfileTxt(Provider p) {
        StringBuilder sb = new StringBuilder();
        sb.append("SkillConnect - Provider Profile\n");
        sb.append("===============================\n\n");
        sb.append("Name: ").append(nz(p.getName())).append('\n');
        sb.append("Profession: ").append(nz(p.getTitle())).append('\n');
        sb.append("Category: ").append(CategoryData.getCategoryName(p.getCategory())).append('\n');
        sb.append("Experience: ").append(p.getExperienceYears()).append(p.getExperienceYears() == 1 ? " year" : " years").append('\n');
        sb.append("Location: ").append(nz(p.getAddress())).append('\n');
        sb.append("Starting Price: ").append(UiUtils.formatPrice(p.getStartingPrice())).append(' ').append(nz(p.getPriceUnit())).append('\n');
        sb.append("Rating: ").append(UiUtils.formatRating(p.getAverageRating())).append(" (").append(UiUtils.reviewCount(p.getReviewCount())).append(")\n");
        sb.append("Availability: ").append(DateTimeUtils.summarizeDays(p.getAvailableDays())).append(", ")
                .append(DateTimeUtils.hoursRange(p.getAvailableFrom(), p.getAvailableTo())).append('\n');
        sb.append("Phone: ").append(nz(p.getPhone())).append('\n');
        sb.append("Email: ").append(nz(p.getEmail())).append('\n');
        sb.append("Verified: ").append(p.isVerified() ? "Yes" : "Pending").append('\n');
        sb.append("Skills: ").append(p.getSkills().isEmpty() ? "-" : android.text.TextUtils.join(", ", p.getSkills())).append("\n\n");
        sb.append("About:\n").append(nz(p.getDescription())).append("\n\n");
        sb.append("Exported on ").append(DateTimeUtils.formatDate(System.currentTimeMillis())).append('\n');
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null || s.trim().isEmpty() ? "-" : s;
    }

    // ------------------------------------------------------------ export UI

    /** Writes the file, reads it back and shows a preview with Share / Save options. */
    public static void exportAndPreview(Activity activity, String fileName, String mimeType, String content) {
        File file;
        String readBack;
        try {
            file = writeTextFile(activity, fileName, content);
            readBack = readTextFile(file);
        } catch (IOException e) {
            UiUtils.toast(activity, "Could not create the file. Please try again.");
            return;
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_file_preview, null);
        ((TextView) view.findViewById(R.id.tvFileName)).setText(fileName);
        ((TextView) view.findViewById(R.id.tvFileInfo)).setText(String.format(Locale.US,
                "%d lines · %.1f KB · saved in app storage", countLines(readBack), file.length() / 1024f));
        ((TextView) view.findViewById(R.id.tvFileContent)).setText(readBack);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("Export ready")
                .setView(view)
                .setPositiveButton(R.string.share, (d, w) -> shareFile(activity, file, mimeType))
                .setNegativeButton(R.string.close, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setNeutralButton("Save to Downloads", (d, w) -> {
                boolean ok = saveToDownloads(activity, fileName, mimeType, readBack);
                UiUtils.toast(activity, ok ? "Saved to Downloads/SkillConnect" : "Could not save the file");
            });
        }
        builder.show();
    }

    private static int countLines(String text) {
        if (text.isEmpty()) return 0;
        return text.split("\n").length;
    }

    /** Implicit intent: share the file with any app (Gmail, Drive, WhatsApp…). */
    public static void shareFile(Context context, File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(intent, "Share " + file.getName()));
        } catch (ActivityNotFoundException e) {
            UiUtils.toast(context, "No app found to share this file");
        }
    }

    /** Saves a copy to the public Downloads folder (Android 10+, no permission needed). */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean saveToDownloads(Context context, String fileName, String mimeType, String content) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SkillConnect");
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return false;
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) return false;
            out.write(content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
