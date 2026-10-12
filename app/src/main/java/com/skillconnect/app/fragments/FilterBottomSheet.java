package com.skillconnect.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.skillconnect.app.R;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.FilterOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters bottom sheet built with classic Android widgets:
 * Spinner (category, distance, sort), RadioButtons (price, rating) and CheckBoxes (availability).
 * The result is returned with the Fragment Result API.
 */
public class FilterBottomSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "filter_request";
    public static final String RESULT_FILTERS = "filters";
    private static final String ARG_FILTERS = "arg_filters";
    private static final String ARG_LOCK_CATEGORY = "arg_lock_category";

    private static final int[] PRICE_IDS = {R.id.rbPriceAny, R.id.rbPrice1, R.id.rbPrice2, R.id.rbPrice3, R.id.rbPrice4};

    private Spinner spinnerCategory;
    private Spinner spinnerDistance;
    private Spinner spinnerSort;
    private RadioGroup rgPrice;
    private RadioGroup rgRating;
    private CheckBox cbAvailable;
    private CheckBox cbVerified;

    private FilterOptions original;
    private boolean lockCategory;
    private final List<Category> categories = CategoryData.getMainCategories();

    public static FilterBottomSheet newInstance(FilterOptions current, boolean lockCategory) {
        FilterBottomSheet sheet = new FilterBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILTERS, current.copy());
        args.putBoolean(ARG_LOCK_CATEGORY, lockCategory);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        original = (FilterOptions) args.getSerializable(ARG_FILTERS);
        if (original == null) original = new FilterOptions();
        lockCategory = args.getBoolean(ARG_LOCK_CATEGORY);

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerDistance = view.findViewById(R.id.spinnerDistance);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        rgPrice = view.findViewById(R.id.rgPrice);
        rgRating = view.findViewById(R.id.rgRating);
        cbAvailable = view.findViewById(R.id.cbAvailable);
        cbVerified = view.findViewById(R.id.cbVerified);

        List<String> categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.all_categories));
        for (Category c : categories) categoryNames.add(c.getName());
        spinnerCategory.setAdapter(spinnerAdapter(categoryNames));
        spinnerCategory.setEnabled(!lockCategory);

        List<String> distances = new ArrayList<>();
        for (String s : getResources().getStringArray(R.array.distance_options)) distances.add(s);
        spinnerDistance.setAdapter(spinnerAdapter(distances));

        List<String> sorts = new ArrayList<>();
        for (String s : getResources().getStringArray(R.array.sort_options)) sorts.add(s);
        spinnerSort.setAdapter(spinnerAdapter(sorts));

        showOptions(original);

        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            FilterOptions reset = new FilterOptions();
            if (lockCategory) reset.categoryId = original.categoryId;
            reset.skill = original.skill;
            showOptions(reset);
        });
        view.findViewById(R.id.btnApply).setOnClickListener(v -> apply());
    }

    private ArrayAdapter<String> spinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner, items);
        adapter.setDropDownViewResource(R.layout.item_spinner);
        return adapter;
    }

    private void showOptions(FilterOptions f) {
        int categoryIndex = 0;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(f.categoryId)) categoryIndex = i + 1;
        }
        spinnerCategory.setSelection(categoryIndex);
        spinnerDistance.setSelection(f.distanceIndex);
        spinnerSort.setSelection(f.sortBy);
        rgPrice.check(PRICE_IDS[Math.max(0, Math.min(f.priceRange, PRICE_IDS.length - 1))]);
        if (f.minRating >= 4.5) rgRating.check(R.id.rbRating45);
        else if (f.minRating >= 4.0) rgRating.check(R.id.rbRating4);
        else rgRating.check(R.id.rbRatingAny);
        cbAvailable.setChecked(f.availableOnly);
        cbVerified.setChecked(f.verifiedOnly);
    }

    private void apply() {
        FilterOptions f = new FilterOptions();
        int categoryIndex = spinnerCategory.getSelectedItemPosition();
        f.categoryId = categoryIndex > 0 ? categories.get(categoryIndex - 1).getId() : null;
        f.skill = original.skill;
        f.distanceIndex = spinnerDistance.getSelectedItemPosition();
        f.sortBy = spinnerSort.getSelectedItemPosition();
        int priceId = rgPrice.getCheckedRadioButtonId();
        for (int i = 0; i < PRICE_IDS.length; i++) if (PRICE_IDS[i] == priceId) f.priceRange = i;
        int ratingId = rgRating.getCheckedRadioButtonId();
        f.minRating = ratingId == R.id.rbRating45 ? 4.5 : ratingId == R.id.rbRating4 ? 4.0 : 0;
        f.availableOnly = cbAvailable.isChecked();
        f.verifiedOnly = cbVerified.isChecked();

        Bundle result = new Bundle();
        result.putSerializable(RESULT_FILTERS, f);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}
