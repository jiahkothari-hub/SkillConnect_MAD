package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.CategoryAdapter;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.CategoryData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.Category;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.utils.StateView;

import java.util.List;

/** All categories and skills, with the number of providers in each. */
public class CategoriesActivity extends AppCompatActivity {

    private CategoryAdapter adapter;
    private StateView state;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        listView = findViewById(R.id.listCategories);
        state = new StateView(findViewById(R.id.categoriesState));
        adapter = new CategoryAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Category c = adapter.getItem(position);
            Intent i = new Intent(this, ProviderListActivity.class);
            i.putExtra(ProviderListActivity.EXTRA_TITLE, c.getName());
            if (c.isMainCategory()) {
                i.putExtra(ProviderListActivity.EXTRA_CATEGORY_ID, c.getId());
            } else {
                i.putExtra(ProviderListActivity.EXTRA_CATEGORY_ID, c.getParentId());
                i.putExtra(ProviderListActivity.EXTRA_SKILL, c.getName());
            }
            startActivity(i);
        });
        load();
    }

    private void load() {
        listView.setVisibility(View.GONE);
        state.showLoading(getString(R.string.loading));
        RepositoryProvider.get().getProviders(false, new Callback<List<Provider>>() {
            @Override
            public void onSuccess(List<Provider> providers) {
                List<Category> tree = CategoryData.getCategoryTree();
                for (Category c : tree) {
                    c.setProviderCount(c.isMainCategory()
                            ? CategoryData.countForCategory(providers, c.getId())
                            : CategoryData.countForSkill(providers, c.getName()));
                }
                adapter.submit(tree);
                state.hide();
                listView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                state.showError(message, CategoriesActivity.this::load);
            }
        });
    }
}
