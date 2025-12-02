package com.example.cafelariapos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextFirstName, editTextLastName, editTextEmail, editTextUsername, editTextPassword, editTextMobile;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ensure background drawable is applied at runtime (defensive)
        View root = findViewById(R.id.rootLayoutRegister);
        if (root != null) {
            root.setBackgroundResource(R.drawable.cafelariabackground);
        }

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        gson = new Gson();

        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextRegUsername);
        editTextPassword = findViewById(R.id.editTextRegPassword);
        // Direct lookup for mobile EditText
        editTextMobile = findViewById(R.id.editTextMobile);

        findViewById(R.id.buttonRegister).setOnClickListener(v -> register());
    }

    private void register() {
        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String mobile = editTextMobile != null ? editTextMobile.getText().toString().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        String usersJson = sharedPreferences.getString("users", null);
        List<UserData> userList;
        if (usersJson != null) {
            userList = gson.fromJson(usersJson, new com.google.gson.reflect.TypeToken<List<UserData>>(){}.getType());
            if (userList == null) userList = new ArrayList<>();
        } else {
            userList = new ArrayList<>();
        }

        for (UserData user : userList) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        UserData userData = new UserData(email, firstName, lastName, mobile, username, password);
        userList.add(userData);

        String updatedUsersJson = gson.toJson(userList);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("users", updatedUsersJson);
        editor.apply();

        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearAllUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, "All user data cleared", Toast.LENGTH_SHORT).show();
    }
}
