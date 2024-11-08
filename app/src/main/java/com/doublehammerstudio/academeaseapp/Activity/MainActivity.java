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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
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
        db = FirebaseFirestore.getInstance();

        scanButton = findViewById(R.id.scanButton);
        attendanceButton = findViewById(R.id.attendanceButton);
        registerStudentButton = findViewById(R.id.registerStudent);
        logoutButton = findViewById(R.id.logoutButton);

        userEmailTextView = findViewById(R.id.userEmailText);

        // Set attendance button to disabled and gray by default
        attendanceButton.setEnabled(false);
        attendanceButton.setAlpha(0.5f); // make it appear gray

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userEmailTextView.setText(user.getEmail() != null ? user.getEmail() : "User");
            Toast.makeText(MainActivity.this, "Welcome, " + (user.getEmail() != null ? user.getEmail() : "User"), Toast.LENGTH_SHORT).show();
            checkAttendanceEligibility(user.getUid());
        }

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
    }

    private void checkAttendanceEligibility(String currentUserID) {
        // Get the current day of the week
        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // Get the current date in "yyyy-MM-dd" format
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());



        // Query Firestore for matching day, teacherUID, and section
        db.collection("sections")
                .whereEqualTo("day", currentDay)
                .whereEqualTo("teacherUID", currentUserID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isWithinAttendanceWindow = false; // Track if current time is within any attendance window

                    Calendar currentTime = Calendar.getInstance();
                    Calendar attendanceWindowEnd = null;

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String startTimeStr = document.getString("startTime");
                        String section_str = document.getString("section");

                        if (startTimeStr != null) {
                            try {
                                // Confirm the startTime string is fetched correctly
                                Toast.makeText(MainActivity.this, "Start Time: " + startTimeStr + " " + section_str, Toast.LENGTH_SHORT).show();

                                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                                // Parse the start time and set it to today’s date to avoid date mismatches
                                Calendar startTime = Calendar.getInstance();
                                startTime.setTime(timeFormat.parse(startTimeStr));
                                startTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
                                startTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
                                startTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));

                                // Calculate the 30-minute attendance window start time
                                Calendar attendanceWindowStart = (Calendar) startTime.clone();
                                attendanceWindowStart.add(Calendar.MINUTE, -30);

                                // Calculate the 15-minute window after start time
                                attendanceWindowEnd = (Calendar) startTime.clone();
                                attendanceWindowEnd.add(Calendar.MINUTE, 15);

                                // Check if current time is within the attendance window
                                if (currentTime.after(attendanceWindowStart) && currentTime.before(attendanceWindowEnd)) {
                                    // Enable button for the first valid subject found
                                    isWithinAttendanceWindow = true;
                                }

                                // If current time is more than 15 minutes after the start time, mark student as absent
                                if (currentTime.after(attendanceWindowEnd)) {
                                    Toast.makeText(MainActivity.this, "Closed - Marked Absent", Toast.LENGTH_SHORT).show();

                                    // Query Firestore to find attendance record for the current section and teacherUID
                                    db.collection("attendance")
                                            .whereEqualTo("section", section_str)
                                            .whereEqualTo("teacherUID", currentUserID)
                                            .get()
                                            .addOnSuccessListener(attendanceSnapshots -> {
                                                for (DocumentSnapshot attendanceDoc : attendanceSnapshots) {
                                                    List<Map<String, Object>> attendanceEntries = (List<Map<String, Object>>) attendanceDoc.get("attendanceEntries");

                                                    boolean hasAttendanceForToday = false;
                                                    if (attendanceEntries != null) {
                                                        for (Map<String, Object> entry : attendanceEntries) {
                                                            String entryDate = (String) entry.get("date");
                                                            if (currentDate.equals(entryDate)) {
                                                                hasAttendanceForToday = true;
                                                                break;
                                                            }
                                                        }
                                                    }

                                                    // If no attendance record exists for the current day, add an absent record
                                                    if (!hasAttendanceForToday) {
                                                        // Get the current time in "HH:mm" format
                                                        SimpleDateFormat timeFormat123 = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                                        String AbsetcurrentTimeStr = timeFormat123.format(Calendar.getInstance().getTime());

                                                        Map<String, Object> absentEntry = new HashMap<>();
                                                        absentEntry.put("date", currentDate);
                                                        absentEntry.put("remarks", "absent");
                                                        absentEntry.put("timeIn", AbsetcurrentTimeStr);

                                                        if (attendanceEntries == null) {
                                                            attendanceEntries = new ArrayList<>();
                                                        }
                                                        attendanceEntries.add(absentEntry);

                                                        // Update the attendance document
                                                        db.collection("attendance").document(attendanceDoc.getId())
                                                                .update("attendanceEntries", attendanceEntries)
                                                                .addOnSuccessListener(aVoid ->
                                                                        Toast.makeText(MainActivity.this, "Absent record added", Toast.LENGTH_SHORT).show()
                                                                )
                                                                .addOnFailureListener(e ->
                                                                        Toast.makeText(MainActivity.this, "Error marking absent", Toast.LENGTH_SHORT).show()
                                                                );
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(MainActivity.this, "Error fetching attendance data", Toast.LENGTH_SHORT).show()
                                            );
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Only enable the button if any subject matched the attendance window
                    if (isWithinAttendanceWindow) {
                        attendanceButton.setEnabled(true);
                        attendanceButton.setAlpha(1.0f); // Enable button and set normal appearance
                    } else {
                        attendanceButton.setEnabled(false);
                        attendanceButton.setAlpha(0.5f); // Disable button and gray out
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error fetching section data", Toast.LENGTH_SHORT).show();
                });
    }




}
