package com.skillconnect.app.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.skillconnect.app.R;
import com.skillconnect.app.models.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * ListView adapter with two view types: a header row for each main category and
 * a row for each skill inside it.
 */
public class CategoryAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SKILL = 1;

    private final Context context;
    private final List<Category> items = new ArrayList<>();

    public CategoryAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<Category> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Category getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isMainCategory() ? TYPE_HEADER : TYPE_SKILL;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Category c = getItem(position);
        boolean header = c.isMainCategory();
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    header ? R.layout.item_category_header : R.layout.item_category_skill, parent, false);
            h = new ViewHolder(convertView);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }
        h.name.setText(c.getName());
        int count = c.getProviderCount();
        if (header) {
            h.count.setText(count == 1 ? "1 provider" : count + " providers");
            h.icon.setImageResource(c.getIconRes());
            h.icon.setColorFilter(ContextCompat.getColor(context, c.getColorRes()));
            h.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, c.getBackgroundRes())));
        } else {
            h.count.setText(String.valueOf(count));
        }
        return convertView;
    }

    static class ViewHolder {
        final TextView name;
        final TextView count;
        final ImageView icon;

        ViewHolder(View v) {
            name = v.findViewById(R.id.tvName);
            count = v.findViewById(R.id.tvCount);
            icon = v.findViewById(R.id.ivIcon);
        }
    }
}
