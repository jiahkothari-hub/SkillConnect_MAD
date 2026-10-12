package com.skillconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.skillconnect.app.R;
import com.skillconnect.app.activities.ChatActivity;
import com.skillconnect.app.adapters.ChatThreadAdapter;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.data.Subscription;
import com.skillconnect.app.models.ChatThread;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.StateView;

import java.util.List;

/** All conversations of the current user, updated in real time. */
public class ChatListFragment extends Fragment {

    private static final String ARG_SHOW_HEADER = "show_header";

    private AppRepository repo;
    private Subscription subscription;
    private ChatThreadAdapter adapter;
    private RecyclerView recyclerView;
    private StateView state;

    public static ChatListFragment newInstance(boolean showHeader) {
        ChatListFragment f = new ChatListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_HEADER, showHeader);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = RepositoryProvider.get();
        boolean showHeader = getArguments() == null || getArguments().getBoolean(ARG_SHOW_HEADER, true);
        view.findViewById(R.id.layoutHeader).setVisibility(showHeader ? View.VISIBLE : View.GONE);
        recyclerView = view.findViewById(R.id.rvChats);
        state = new StateView(view.findViewById(R.id.chatsState));
        adapter = new ChatThreadAdapter(repo.getCurrentUserId(), chat -> {
            Intent i = new Intent(requireContext(), ChatActivity.class);
            i.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.getChatId());
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        state.showLoading(getString(R.string.loading));
        subscription = repo.listenToMyChats(new Callback<List<ChatThread>>() {
            @Override
            public void onSuccess(List<ChatThread> chats) {
                if (getView() == null) return;
                adapter.submit(chats);
                if (chats.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    User me = repo.getCachedUser();
                    state.showEmpty(R.drawable.ic_forum, getString(R.string.no_chats_title),
                            getString(me != null && me.isProvider() ? R.string.no_chats_msg_provider : R.string.no_chats_msg_customer));
                } else {
                    state.hide();
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (getView() == null) return;
                recyclerView.setVisibility(View.GONE);
                state.showError(message, null);
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (subscription != null) subscription.remove();
        super.onDestroyView();
    }
}
