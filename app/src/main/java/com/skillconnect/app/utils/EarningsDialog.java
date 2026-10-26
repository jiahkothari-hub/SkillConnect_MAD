package com.skillconnect.app.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.app.R;
import com.skillconnect.app.models.Booking;

import java.util.List;

/** Monthly earnings breakdown for providers, with a CSV export. */
public final class EarningsDialog {

    private EarningsDialog() {
    }

    public static void show(Activity activity, List<Booking> bookings) {
        EarningsUtils.Summary s = EarningsUtils.calculate(bookings);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_earnings, null);
        ((TextView) view.findViewById(R.id.tvEarningsDialogTotal)).setText(UiUtils.formatPrice(s.total));
        ((TextView) view.findViewById(R.id.tvEarningsDialogSummary)).setText(
                jobs(s.completedJobs) + " completed · " + UiUtils.formatPrice(s.upcoming) + " upcoming");

        double max = 0;
        for (EarningsUtils.Month m : s.months) max = Math.max(max, m.amount);
        ViewGroup container = view.findViewById(R.id.layoutMonths);
        for (EarningsUtils.Month m : s.months) {
            View row = inflater.inflate(R.layout.item_earnings_month, container, false);
            ((TextView) row.findViewById(R.id.tvMonth)).setText(m.label + " · " + jobs(m.jobs));
            ((TextView) row.findViewById(R.id.tvMonthAmount)).setText(UiUtils.formatPrice(m.amount));
            ((ProgressBar) row.findViewById(R.id.progressMonth)).setProgress(max > 0 ? (int) Math.round(m.amount * 100 / max) : 0);
            container.addView(row);
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.earnings_breakdown)
                .setView(view)
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.export_csv, (d, w) -> FileUtils.exportAndPreview(activity,
                        "earnings_report.csv", FileUtils.MIME_CSV, EarningsUtils.buildEarningsCsv(s)))
                .show();
    }

    public static String jobs(int count) {
        return count + (count == 1 ? " job" : " jobs");
    }
}
