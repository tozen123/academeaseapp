package com.doublehammerstudio.academeaseapp.Activity;

import static com.doublehammerstudio.academeaseapp.R.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class AttendanceActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String PREFS_NAME = "IPPrefs";
    private static final String KEY_IP_ADDRESS = "192.168.1.1";

    // Set static IP address directly
    private String facerecogIpAddress; // IP address to be retrieved from Firestore
    private Button captureButton;
    private ProgressBar loadingProgressBar;
    private String currentPhotoPath;

    private Button buttonScanAgain;
    private Button buttonBackToMainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        buttonScanAgain = findViewById(R.id.buttonScanAgain);
        buttonBackToMainMenu = findViewById(R.id.buttonBackToMainMenu);
        loadingProgressBar = findViewById(R.id.loading_progress);
        // Retrieve the IP address from Firestore and proceed to open the camera
        // Set click listener for Scan Again button
        buttonScanAgain.setOnClickListener(v -> dispatchTakePictureIntent());

        // Set click listener for Back to Main Menu button
        buttonBackToMainMenu.setOnClickListener(v -> {
            Intent mainMenuIntent = new Intent(AttendanceActivity.this, MainActivity.class);
            startActivity(mainMenuIntent);
            finish(); // Optional: closes AttendanceActivity after starting MainActivity
        });

        retrieveFacerecogIpAddress();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void retrieveFacerecogIpAddress() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("API").document("documentID")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        facerecogIpAddress = documentSnapshot.getString("facerecog");
                        if (facerecogIpAddress != null) {
                            dispatchTakePictureIntent(); // Proceed to open the camera
                        } else {
                            showError("Facerecog IP address not found.");
                        }
                    } else {
                        showError("Document does not exist.");
                    }
                })
                .addOnFailureListener(e -> showError("Error fetching IP address: " + e.getMessage()));
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CameraCapture", "Error occurred while creating the file: " + ex.getMessage());
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.doublehammerstudio.academeaseapp.fileprovider",  // Use the authority defined in AndroidManifest
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permissions for URI
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File photoFile = new File(currentPhotoPath);
            uploadImage(photoFile);
        }
    }

    private void uploadImage(File imageFile) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        int timeout = 300;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(facerecogIpAddress) // Use the dynamically fetched IP address
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .connectTimeout(timeout, TimeUnit.SECONDS)
                        .writeTimeout(timeout, TimeUnit.SECONDS)
                        .readTimeout(timeout, TimeUnit.SECONDS)
                        .build())
                .build();

        ApiService uploadService = retrofit.create(ApiService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        Call<ResponseBody> call = uploadService.uploadImage(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                loadingProgressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.optString("status").equals("success")) {
                            JSONObject result = jsonResponse.optJSONObject("result");
                            if (result != null) {
                                String fName = result.optString("FName");
                                String lName = result.optString("LName");
                                String mName = result.optString("MName");
                                String lrn = result.optString("lrn");
                                String imageUrl = result.optString("image_url");

                                /*String message = "Match found:\n" +
                                        "First Name: " + fName + "\n" +
                                        "Last Name: " + lName + "\n" +
                                        "Middle Name: " + mName + "\n" +
                                        "LRN: " + lrn;
                                showResultDialog("Success", message);*/

                                queryStudentAndAddAttendanceRecord(fName, lName, mName, lrn, imageUrl);
                            }
                        } else {
                            String message = jsonResponse.optString("message", "No match within tolerance.");
                            showResultDialog("Failure", message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showResultDialog("Error", "Failed to parse server response.");
                    }
                } else {
                    showResultDialog("Upload Failed", "Upload failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingProgressBar.setVisibility(View.GONE);
                showResultDialog("Upload Failed", "Upload failed: " + t.getMessage());
                Log.e("API Error", "Error: " + t.getMessage());
            }
        });
    }
    // Assuming `currentUserID` is the logged-in user's ID from Firebase Auth
    private void queryStudentAndAddAttendanceRecord(String fName, String lName, String mName, String lrn, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String currentTeacherUID = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the current teacher's UID

        db.collection("students")
                .whereEqualTo("lrn", lrn)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String grade = document.getString("grade");
                        String section = document.getString("section");
                        String string_lrn = document.getString("lrn");
                        String studentDocumentId = document.getId(); // Retrieve the student document ID

                        // Now, use the section to retrieve the teacher UID from the "sections" collection
                        db.collection("sections")
                                .whereEqualTo("day", currentDay)
                                .whereEqualTo("section", section) // Filter by section
                                .whereEqualTo("teacherUID", currentTeacherUID) // Filter by current teacher's UID
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (queryDocumentSnapshots.isEmpty()) {
                                        // If no sections are found, prompt the user to register the student
                                        showErrorDialog("Sorry, no face detected. Please register!");
                                    } else {
                                        for (DocumentSnapshot sectionDocument : queryDocumentSnapshots) {
                                            String startTimeStr = sectionDocument.getString("startTime");
                                            String teacher_uid = sectionDocument.getString("teacherUID");

                                            if (startTimeStr != null && teacher_uid != null) {
                                                try {
                                                    // Confirm the startTime string
                                                    Toast.makeText(AttendanceActivity.this, "Start Time: " + startTimeStr, Toast.LENGTH_SHORT).show();

                                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                                    Calendar currentTime = Calendar.getInstance();

                                                    // Parse the start time and set it to today’s date
                                                    Calendar startTime = Calendar.getInstance();
                                                    startTime.setTime(timeFormat.parse(startTimeStr));
                                                    startTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
                                                    startTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
                                                    startTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));

                                                    // Display the formatted current time
                                                    String currentTimeStr = timeFormat.format(currentTime.getTime());
                                                    Toast.makeText(AttendanceActivity.this, "Current Time: " + currentTimeStr, Toast.LENGTH_SHORT).show();

                                                    // Calculate the 30-minute attendance window before start time
                                                    Calendar attendanceWindowStart = (Calendar) startTime.clone();
                                                    attendanceWindowStart.add(Calendar.MINUTE, -30);

                                                    // Calculate the 15-minute window after start time
                                                    Calendar attendanceWindowEnd = (Calendar) startTime.clone();
                                                    attendanceWindowEnd.add(Calendar.MINUTE, 15);

                                                    // Now, check if the student already has an attendance record for today
                                                    db.collection("attendance")
                                                            .whereEqualTo("section", section)
                                                            .whereEqualTo("teacherUID", teacher_uid)
                                                            .whereEqualTo("studentId", studentDocumentId)
                                                            .get()
                                                            .addOnSuccessListener(attendanceSnapshots -> {
                                                                boolean hasAttendanceForToday = false;

                                                                for (DocumentSnapshot attendanceDoc : attendanceSnapshots) {
                                                                    List<Map<String, Object>> attendanceEntries = (List<Map<String, Object>>) attendanceDoc.get("attendanceEntries");

                                                                    if (attendanceEntries != null) {
                                                                        for (Map<String, Object> entry : attendanceEntries) {
                                                                            String entryDate = (String) entry.get("date");
                                                                            if (currentDate.equals(entryDate)) {
                                                                                hasAttendanceForToday = true;
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                // If the student does not have an attendance record for today, add it
                                                                if (!hasAttendanceForToday) {
                                                                    String remarks = "absent"; // Default to absent
                                                                    if (!currentTime.before(attendanceWindowStart) && currentTime.before(startTime)) {
                                                                        remarks = "present"; // Within the 30-minute window before start time
                                                                    } else if (!currentTime.before(startTime) && currentTime.before(attendanceWindowEnd)) {
                                                                        remarks = "late"; // Within the 15-minute window after start time
                                                                    }

                                                                    // Add the attendance record
                                                                    addAttendanceRecord(fName, lName, mName, studentDocumentId, grade, section, imageUrl, remarks, string_lrn);
                                                                    Toast.makeText(AttendanceActivity.this, "Attendance recorded: " + remarks, Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    String message = "Attendance already recorded for today";
                                                                    showResultDialog("Success", message);

                                                                    Toast.makeText(AttendanceActivity.this, "Attendance already recorded for today", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(AttendanceActivity.this, "Error fetching attendance data", Toast.LENGTH_SHORT).show();
                                                            });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                Toast.makeText(AttendanceActivity.this, "Teacher UID or Start Time missing", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AttendanceActivity.this, "Error fetching section data", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // If no student record found, prompt the user to register the student
                        showErrorDialog("Sorry, no face detected. Please register!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error querying student data", e);
                    showResultDialog("Error", "Failed to retrieve student data.");
                });
    }

    // Method to show the error message with an "Okay" button
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(AttendanceActivity.this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("Okay", (dialog, which) -> {
                    dialog.dismiss(); // Close the dialog when "Okay" is pressed
                })
                .show();
    }





    private void addAttendanceRecord(String fName, String lName, String mName, String studentDocumentId,
                                     String grade, String section, String imageUrl, String Remarks, String lrn) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create an entry for the attendance record
        Map<String, Object> attendanceEntry = new HashMap<>();
        attendanceEntry.put("timeIn", new SimpleDateFormat("HH:mm").format(new Date()));
        attendanceEntry.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        attendanceEntry.put("remarks", Remarks);

        // Display the data in XML layout
        TextView textFirstName = findViewById(R.id.textFirstName);
        TextView textLastName = findViewById(R.id.textLastName);
        TextView textMiddleName = findViewById(R.id.textMiddleName);
        TextView textLRN = findViewById(R.id.textLRN);
        TextView textRemark = findViewById(R.id.textRemark);
        ImageView studentImage = findViewById(R.id.studentImage);

        textFirstName.setText("First Name: " + fName);
        textLastName.setText("Last Name: " + lName);
        textMiddleName.setText("Middle Name: " + mName);
        textLRN.setText("LRN: " + lrn);
        textRemark.setText("Remarks: " + Remarks);

        // Load student image with Glide or Picasso
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_image) // optional placeholder image
                .error(R.drawable.ic_default_image) // optional error image
                .into(studentImage);

        // Set up the main document if it doesn't exist and add attendance to the array
        db.collection("attendance")
                .document(studentDocumentId)
                .update("attendanceEntries", FieldValue.arrayUnion(attendanceEntry))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Attendance record added for student ID: " + studentDocumentId);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseFirestoreException &&
                            ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.NOT_FOUND) {

                        // Document does not exist, create a new one with the array
                        Map<String, Object> newAttendanceData = new HashMap<>();
                        newAttendanceData.put("FName", fName);
                        newAttendanceData.put("LName", lName);
                        newAttendanceData.put("MName", mName);
                        newAttendanceData.put("grade", grade);
                        newAttendanceData.put("section", section);
                        newAttendanceData.put("studentId", studentDocumentId);
                        newAttendanceData.put("teacherUID", getCurrentUserUID());
                        newAttendanceData.put("attendanceEntries", Arrays.asList(attendanceEntry));
                        newAttendanceData.put("image", imageUrl);// Initialize with the first entry

                        db.collection("attendance")
                                .document(studentDocumentId)
                                .set(newAttendanceData)
                                .addOnSuccessListener(innerVoid -> Log.d("Firestore", "New attendance document created with ID: " + studentDocumentId))
                                .addOnFailureListener(innerE -> Log.w("Firestore", "Error creating new attendance document", innerE));
                    } else {
                        Log.w("Firestore", "Error updating attendance record", e);
                    }
                });
    }




    private String getCurrentUserUID() {
        // Replace this with actual code to get the current logged-in user's UID
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }



    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Navigate back to the main menu
                    Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();  // Close the current activity
                })
                .show();
    }

    private interface ApiService {
        @Multipart
        @POST("upload_image")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
    }
}