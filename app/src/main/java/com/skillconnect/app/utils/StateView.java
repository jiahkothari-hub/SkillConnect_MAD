package com.skillconnect.app.utils;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;

/**
 * Controls the reusable loading / empty / error block (layout/view_state.xml)
 * that every list screen includes.
 */
public class StateView {

    private final View root;
    private final ProgressBar progress;
    private final ImageView icon;
    private final TextView title;
    private final TextView message;
    private final MaterialButton action;

    public StateView(View root) {
        this.root = root;
        progress = root.findViewById(R.id.stateProgress);
        icon = root.findViewById(R.id.stateIcon);
        title = root.findViewById(R.id.stateTitle);
        message = root.findViewById(R.id.stateMessage);
        action = root.findViewById(R.id.stateAction);
    }

    public void showLoading(String text) {
        root.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        icon.setVisibility(View.GONE);
        title.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
        message.setText(text);
        action.setVisibility(View.GONE);
    }

    public void showEmpty(@DrawableRes int iconRes, String titleText, String messageText) {
        showEmpty(iconRes, titleText, messageText, null, null);
    }

    public void showEmpty(@DrawableRes int iconRes, String titleText, String messageText,
                          String actionText, Runnable onAction) {
        root.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(iconRes);
        title.setVisibility(View.VISIBLE);
        title.setText(titleText);
        message.setVisibility(messageText != null ? View.VISIBLE : View.GONE);
        message.setText(messageText);
        if (actionText != null && onAction != null) {
            action.setVisibility(View.VISIBLE);
            action.setText(actionText);
            action.setOnClickListener(v -> onAction.run());
        } else {
            action.setVisibility(View.GONE);
        }
    }

    public void showError(String errorMessage, Runnable onRetry) {
        showEmpty(R.drawable.ic_info, "Couldn't load this", errorMessage,
                onRetry != null ? root.getContext().getString(R.string.retry) : null, onRetry);
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }
}
