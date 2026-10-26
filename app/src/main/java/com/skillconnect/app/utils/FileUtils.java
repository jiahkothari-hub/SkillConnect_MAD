package com.skillconnect.app.utils;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
 * back for a preview, saves a copy to Downloads that can be opened from the app or the Files
 * app, and shares them with an implicit ACTION_SEND intent (via FileProvider).
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

    /** Writes the file, reads it back and shows a preview with Save / Share options. */
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
                "%d lines · %.1f KB", countLines(readBack), file.length() / 1024f));
        ((TextView) view.findViewById(R.id.tvFileContent)).setText(
                MIME_CSV.equals(mimeType) ? formatCsvAsTable(readBack) : readBack);

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Export ready")
                .setView(view)
                .setPositiveButton(R.string.save_to_downloads, (d, w) -> {
                    Uri saved = saveToDownloads(activity, fileName, mimeType, readBack);
                    if (saved == null) {
                        UiUtils.toast(activity, "Could not save the file");
                    } else {
                        showSavedDialog(activity, saved, fileName, mimeType, readBack);
                    }
                })
                .setNeutralButton(R.string.share, (d, w) -> shareFile(activity, file, mimeType))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private static int countLines(String text) {
        if (text.isEmpty()) return 0;
        return text.split("\n").length;
    }

    /** Confirms where the file was saved and lets the user open it straight away. */
    private static void showSavedDialog(Activity activity, Uri uri, String fileName, String mimeType, String content) {
        String location = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? "Downloads › SkillConnect › " + fileName
                : "Android › data › " + activity.getPackageName() + " › files › Download › " + fileName;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("File saved")
                .setMessage("Saved to:\n" + location
                        + "\n\nTap Open to view it now, or find it later in the Files app.")
                .setPositiveButton(R.string.open_file, (d, w) -> openFile(activity, uri, fileName, mimeType, content))
                .setNegativeButton(R.string.close, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setNeutralButton(R.string.show_in_downloads, (d, w) -> {
                try {
                    activity.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException e) {
                    UiUtils.toast(activity, "Open the Files app and go to Downloads › SkillConnect");
                }
            });
        }
        builder.show();
    }

    /**
     * Opens the saved file with another app (implicit ACTION_VIEW). Many phones and emulators have no
     * spreadsheet app, so we fall back to any text viewer and finally to an in-app viewer.
     */
    public static void openFile(Activity activity, Uri uri, String fileName, String mimeType, String content) {
        for (String type : new String[]{mimeType, MIME_TXT}) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                activity.startActivity(intent);
                return;
            } catch (ActivityNotFoundException ignored) {
                // try the next type
            }
        }
        showInAppViewer(activity, fileName, mimeType, content);
    }

    private static void showInAppViewer(Activity activity, String fileName, String mimeType, String content) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_file_preview, null);
        ((TextView) view.findViewById(R.id.tvFileName)).setText(fileName);
        ((TextView) view.findViewById(R.id.tvFileInfo)).setText("No app on this phone opens " + extension(fileName)
                + " files, so it is shown here. Install Google Sheets or Excel to open it as a spreadsheet.");
        ((TextView) view.findViewById(R.id.tvFileContent)).setText(
                MIME_CSV.equals(mimeType) ? formatCsvAsTable(content) : content);
        new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot).toUpperCase(Locale.US) : "these";
    }

    /** Lines CSV columns up so the preview reads like a table. */
    static String formatCsvAsTable(String csvText) {
        List<List<String>> rows = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        for (String line : csvText.split("\n")) {
            List<String> cells = parseCsvLine(line);
            for (int i = 0; i < cells.size(); i++) {
                if (widths.size() <= i) widths.add(0);
                widths.set(i, Math.max(widths.get(i), cells.get(i).length()));
            }
            rows.add(cells);
        }
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            List<String> cells = rows.get(r);
            for (int i = 0; i < cells.size(); i++) {
                sb.append(cells.get(i));
                if (i < cells.size() - 1) pad(sb, ' ', widths.get(i) - cells.get(i).length() + 2);
            }
            sb.append('\n');
            if (r == 0) {
                int total = 0;
                for (int w : widths) total += w + 2;
                pad(sb, '-', total - 2);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** String.repeat() needs Android 13, so pad manually (minSdk 26). */
    private static void pad(StringBuilder sb, char ch, int count) {
        for (int i = 0; i < count; i++) sb.append(ch);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
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

    /**
     * Saves a copy the user can find in the Files app and returns its Uri (null on failure).
     * Android 10+: public Downloads/SkillConnect via MediaStore (no permission needed), replacing an
     * older export with the same name. Android 8–9: the app's own Download folder, shared via FileProvider.
     */
    public static Uri saveToDownloads(Context context, String fileName, String mimeType, String content) {
        // A byte-order mark lets Excel / Sheets read the ₹ sign and names correctly
        byte[] bytes = ((MIME_CSV.equals(mimeType) ? "\uFEFF" : "") + content).getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveToMediaStore(context, fileName, mimeType, bytes);
        }
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null || (!dir.exists() && !dir.mkdirs())) return null;
        File file = new File(dir, fileName);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            return null;
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static Uri saveToMediaStore(Context context, String fileName, String mimeType, byte[] bytes) {
        ContentResolver resolver = context.getContentResolver();
        String folder = Environment.DIRECTORY_DOWNLOADS + "/SkillConnect/";
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        // Replace our previous export instead of creating "booking_history (1).csv"
        try (Cursor c = resolver.query(collection, new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                new String[]{fileName, folder}, null)) {
            while (c != null && c.moveToNext()) {
                resolver.delete(ContentUris.withAppendedId(collection, c.getLong(0)), null, null);
            }
        } catch (RuntimeException ignored) {
            // Not our file (e.g. left over from a previous install) – a new copy is created instead
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, folder);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = resolver.insert(collection, values);
        if (uri == null) return null;
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) throw new IOException("No output stream");
            out.write(bytes);
        } catch (IOException e) {
            resolver.delete(uri, null, null);
            return null;
        }
        // Finished writing – make the file visible to other apps
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }
}
