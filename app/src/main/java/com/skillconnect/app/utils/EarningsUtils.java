package com.skillconnect.app.utils;

import com.skillconnect.app.models.Booking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Calculates a provider's earnings from their bookings (completed jobs count as earned). */
public final class EarningsUtils {

    /** Number of months shown in the monthly breakdown (including the current month). */
    public static final int BREAKDOWN_MONTHS = 6;

    private EarningsUtils() {
    }

    public static class Summary {
        public double total;           // all completed jobs
        public double thisMonth;       // completed jobs scheduled in the current calendar month
        public double upcoming;        // accepted jobs not yet completed (expected income)
        public int completedJobs;
        public int completedThisMonth;
        public final List<Month> months = new ArrayList<>();   // newest first

        public double averagePerJob() {
            return completedJobs == 0 ? 0 : total / completedJobs;
        }
    }

    public static class Month {
        public final String label;     // e.g. "Oct 2026"
        public double amount;
        public int jobs;

        Month(String label) {
            this.label = label;
        }
    }

    public static Summary calculate(List<Booking> bookings) {
        Summary s = new Summary();
        Calendar now = Calendar.getInstance();
        int currentIndex = monthIndex(now);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < BREAKDOWN_MONTHS; i++) {
            c.setTimeInMillis(now.getTimeInMillis());
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, -i);
            s.months.add(new Month(fmt.format(c.getTime())));
        }
        if (bookings == null) return s;

        for (Booking b : bookings) {
            if (Booking.STATUS_ACCEPTED.equals(b.getStatus())) {
                s.upcoming += b.getPrice();
            } else if (Booking.STATUS_COMPLETED.equals(b.getStatus())) {
                s.total += b.getPrice();
                s.completedJobs++;
                c.setTimeInMillis(b.getScheduledAt());
                int monthsAgo = currentIndex - monthIndex(c);
                if (monthsAgo == 0) {
                    s.thisMonth += b.getPrice();
                    s.completedThisMonth++;
                }
                if (monthsAgo >= 0 && monthsAgo < BREAKDOWN_MONTHS) {
                    Month m = s.months.get(monthsAgo);
                    m.amount += b.getPrice();
                    m.jobs++;
                }
            }
        }
        return s;
    }

    private static int monthIndex(Calendar c) {
        return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH);
    }

    /** earnings_report.csv: Month,Completed jobs,Earnings (INR) + totals. */
    public static String buildEarningsCsv(Summary s) {
        StringBuilder sb = new StringBuilder("Month,Completed jobs,Earnings (INR)\n");
        for (Month m : s.months) {
            sb.append(m.label).append(',').append(m.jobs).append(',')
                    .append(String.format(Locale.US, "%.0f", m.amount)).append('\n');
        }
        sb.append('\n');
        sb.append("Total earned,").append(s.completedJobs).append(',')
                .append(String.format(Locale.US, "%.0f", s.total)).append('\n');
        sb.append("Upcoming (accepted jobs),,").append(String.format(Locale.US, "%.0f", s.upcoming)).append('\n');
        return sb.toString();
    }
}
