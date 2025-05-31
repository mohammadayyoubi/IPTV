package com.example.iptv.adapters.admin;
/*
Note that this page till now is only stimulating but not real controls the users.
 Firebase dont allow the client side to manage it
 you should creat admin sdk firebase - cloud function backend through js  and json
 then deploy it to firebase*/
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptv.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    private List<User> userList;
    private final LayoutInflater inflater;
    private final OnUserActionListener listener;

    public UserAdapter(List<User> userList, LayoutInflater inflater, OnUserActionListener listener) {
        this.userList = userList;
        this.inflater = inflater;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.emailTextView.setText(user.email);
        holder.editButton.setOnClickListener(v -> listener.onEdit(user));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        ImageButton editButton, deleteButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.userEmailTextView);
            editButton = itemView.findViewById(R.id.editUserButton);
            deleteButton = itemView.findViewById(R.id.deleteUserButton);
        }
    }

    public static class User {
        public String uid;
        public String email;

        public User(String uid, String email) {
            this.uid = uid;
            this.email = email;
        }
    }
}
