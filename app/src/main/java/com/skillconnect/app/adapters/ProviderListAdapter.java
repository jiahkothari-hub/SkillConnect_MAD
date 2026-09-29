package com.skillconnect.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.skillconnect.app.R;
import com.skillconnect.app.database.DatabaseHelper;
import com.skillconnect.app.models.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom ListView adapter (BaseAdapter + ViewHolder pattern) that shows provider cards
 * with image, name, skill, rating, price and distance. Used by the provider listing and
 * the search results.
 */
public class ProviderListAdapter extends BaseAdapter {

    private final Context context;
    private final List<Provider> providers = new ArrayList<>();
    private final DatabaseHelper db;
    private final String userId;
    private final ProviderCardHolder.Listener listener;

    public ProviderListAdapter(Context context, String userId, ProviderCardHolder.Listener listener) {
        this.context = context;
        this.db = DatabaseHelper.getInstance(context);
        this.userId = userId;
        this.listener = listener;
    }

    public void submit(List<Provider> list) {
        providers.clear();
        providers.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return providers.size();
    }

    @Override
    public Provider getItem(int position) {
        return providers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ProviderCardHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_provider_card, parent, false);
            holder = new ProviderCardHolder(convertView);
            convertView.setTag(holder);        // ViewHolder pattern: views are looked up once
        } else {
            holder = (ProviderCardHolder) convertView.getTag();
        }
        Provider p = getItem(position);
        holder.bind(p, db.isFavorite(userId, p.getProviderId()), listener);
        return convertView;
    }
}
