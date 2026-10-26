package com.example.iptv.activities.admin;
/*
Note that this page till now is only stimulating but not real controls the users.
 Firebase dont allow the client side to manage it
 you should creat admin sdk firebase - cloud function backend through js  and json
 then deploy it to firebase*/
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.*;

import com.example.iptv.R;
import com.example.iptv.adapters.admin.UserAdapter;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.InputDialog;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class activity_user_management extends BaseActivity {

    private RecyclerView userRecyclerView;
    private EditText searchEditText;
    private UserAdapter adapter;
    private List<UserAdapter.User> userList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private TextView countText;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        UiUtils.applyInsets(findViewById(R.id.root), true, true);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        userRecyclerView = findViewById(R.id.userRecyclerView);
        searchEditText = findViewById(R.id.AllChannelsSearchEditText);
        countText = findViewById(R.id.countText);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        adapter = new UserAdapter(userList, getLayoutInflater(), new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(UserAdapter.User user) {
                showUserDialog(user);
            }

            @Override
            public void onDelete(UserAdapter.User user) {
                InputDialog.confirm(activity_user_management.this, getString(R.string.adm_delete_admin),
                        getString(R.string.adm_delete_admin_body, user.email), getString(R.string.delete), true, () -> {
                            FirebaseAuth.getInstance().getCurrentUser().delete(); // placeholder
                            Toast.makeText(activity_user_management.this, getString(R.string.adm_delete_stub), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        UiUtils.setupGrid(userRecyclerView, R.dimen.tile_min_admin_row);
        userRecyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton addButton = findViewById(R.id.addUserButton);
        addButton.setOnClickListener(v -> showUserDialog(null));
        UiUtils.shrinkFabOnScroll(userRecyclerView, addButton);

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
        new InputDialog(this)
                .title(existingUser == null ? getString(R.string.add_admin) : getString(R.string.adm_edit_admin))
                .icon(R.drawable.ic_person)
                .field(getString(R.string.email), existingUser != null ? existingUser.email : null)
                .fieldInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .onSubmit((email, unused) -> {
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
                })
                .show();
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
            filter(searchEditText.getText() == null ? "" : searchEditText.getText().toString());
        });
    }

    private void filter(String text) {
        List<UserAdapter.User> filtered = new ArrayList<>();
        for (UserAdapter.User user : userList) {
            if (user.email.toLowerCase().contains(text.toLowerCase().trim())) {
                filtered.add(user);
            }
        }
        adapter.updateList(filtered);

        countText.setText(UiUtils.quantity(this, R.plurals.count_admins, userList.size()));
        if (userList.isEmpty()) {
            UiUtils.showEmptyState(emptyState, true, R.drawable.ic_group,
                    R.string.empty_admins_title, R.string.empty_admins_body);
        } else {
            UiUtils.showEmptyState(emptyState, filtered.isEmpty(), R.drawable.ic_search,
                    R.string.empty_search_title, R.string.empty_search_body);
        }
    }
}
