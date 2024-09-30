package com.doublehammerstudio.academeaseapp.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ImageView scanButton, attendanceButton, registerStudentButton;
    private Button logoutButton;
    private TextView userEmailTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        scanButton = findViewById(R.id.scanButton);
        attendanceButton = findViewById(R.id.attendanceButton);
        registerStudentButton = findViewById(R.id.registerStudent);
        logoutButton = findViewById(R.id.logoutButton);

        userEmailTextView = findViewById(R.id.userEmailText);

        scanButton.setOnClickListener(view -> {
            Intent scanIntent = new Intent(MainActivity.this, ScanExamChooseTestActivity.class);
            startActivity(scanIntent);
        });

        attendanceButton.setOnClickListener(view -> {
            Intent attendanceIntent = new Intent(MainActivity.this, AttendanceActivity.class);
            startActivity(attendanceIntent);
        });

        registerStudentButton.setOnClickListener(view -> {
            Intent registerIntent = new Intent(MainActivity.this, RegisterStudentActivity.class);
            startActivity(registerIntent);
        });

        logoutButton.setOnClickListener(view -> {
            mAuth.signOut();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        });

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userEmailTextView.setText(user.getEmail().toString());
            Toast.makeText(MainActivity.this, "Welcome, " + (user.getEmail()!= null ? user.getEmail() : "User"), Toast.LENGTH_SHORT).show();
        }
    }




}
