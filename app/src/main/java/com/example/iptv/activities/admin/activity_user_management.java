package com.example.iptv.activities.admin;
/*
Note that this page till now is only stimulating but not real controls the users.
 Firebase dont allow the client side to manage it
 you should creat admin sdk firebase - cloud function backend through js  and json
 then deploy it to firebase*/
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.example.iptv.R;
import com.example.iptv.adapters.admin.UserAdapter;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class activity_user_management extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private EditText searchEditText;
    private UserAdapter adapter;
    private List<UserAdapter.User> userList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        userRecyclerView = findViewById(R.id.userRecyclerView);
        searchEditText = findViewById(R.id.AllChannelsSearchEditText);
        Button addButton = findViewById(R.id.addUserButton);

        adapter = new UserAdapter(userList, getLayoutInflater(), new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(UserAdapter.User user) {
                showUserDialog(user);
            }

            @Override
            public void onDelete(UserAdapter.User user) {
                new AlertDialog.Builder(activity_user_management.this)
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete " + user.email + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseAuth.getInstance().getCurrentUser().delete(); // placeholder
                            Toast.makeText(activity_user_management.this, "Deletion request (stub)", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userRecyclerView.setAdapter(adapter);

        addButton.setOnClickListener(v -> showUserDialog(null));

        searchEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        loadUsersFromFirestore(); // Replace with admin metadata from Firestore
    }

    private void showUserDialog(UserAdapter.User existingUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingUser == null ? "Add Admin" : "Edit Admin");

        final EditText input = new EditText(this);
        input.setHint("Email");
        if (existingUser != null) input.setText(existingUser.email);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty()) return;

            if (existingUser == null) {
                // You should also request password or send invite
                Map<String, Object> newUser = new HashMap<>();
                newUser.put("email", email);
                firestore.collection("adminUsers").add(newUser);
            } else {
                firestore.collection("adminUsers").document(existingUser.uid).update("email", email);
            }
            loadUsersFromFirestore();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadUsersFromFirestore() {
        firestore.collection("adminUsers").get().addOnSuccessListener(query -> {
            userList.clear();
            for (QueryDocumentSnapshot doc : query) {
                String id = doc.getId();
                String email = doc.getString("email");
                if (email != null) {
                    userList.add(new UserAdapter.User(id, email));
                }
            }
            adapter.updateList(userList);
        });
    }

    private void filter(String text) {
        List<UserAdapter.User> filtered = new ArrayList<>();
        for (UserAdapter.User user : userList) {
            if (user.email.toLowerCase().contains(text.toLowerCase())) {
                filtered.add(user);
            }
        }
        adapter.updateList(filtered);
    }
}
