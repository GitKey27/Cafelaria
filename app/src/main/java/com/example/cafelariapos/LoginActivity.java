package com.example.cafelariapos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ensure background drawable is applied at runtime (defensive)
        View root = findViewById(R.id.rootLayout);
        if (root != null) {
            root.setBackgroundResource(R.drawable.cafelariabackground);
        }

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        gson = new Gson();

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);

        findViewById(R.id.buttonLogin).setOnClickListener(v -> login());
        findViewById(R.id.buttonGoToRegister).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void login() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String usersJson = sharedPreferences.getString("users", null);
        List<UserData> userList = new ArrayList<>();
        if (usersJson != null) {
            userList = gson.fromJson(usersJson, new TypeToken<List<UserData>>(){}.getType());
            if (userList == null) userList = new ArrayList<>();
        }

        UserData matched = null;
        for (UserData u : userList) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(username)) {
                matched = u;
                break;
            }
        }

        if (matched != null && matched.getPassword() != null && matched.getPassword().equals(password)) {
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra("username", matched.getUsername());
            intent.putExtra("firstName", matched.getFirstName());
            intent.putExtra("email", matched.getEmailAddress());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }
}
