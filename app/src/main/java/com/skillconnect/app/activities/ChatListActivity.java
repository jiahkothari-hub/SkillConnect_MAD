package com.skillconnect.app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.app.R;
import com.skillconnect.app.fragments.ChatListFragment;

/** Customer's Messages screen (providers have Messages as a bottom-navigation tab instead). */
public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.messages);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chatListContainer, ChatListFragment.newInstance(false))
                    .commit();
        }
    }
}
