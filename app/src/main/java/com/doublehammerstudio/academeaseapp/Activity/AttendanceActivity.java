package com.doublehammerstudio.academeaseapp.Activity;

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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

    private Button captureButton;
    private ProgressBar loadingProgressBar;
    private String currentPhotoPath;
    private String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        captureButton = findViewById(R.id.capture_button);
        loadingProgressBar = findViewById(R.id.loading_progress);

        captureButton.setOnClickListener(v -> showIpInputDialog());
    }

    private void showIpInputDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_ip_input, null);
        final EditText ipEditText = dialogView.findViewById(R.id.ipEditText);
        final CheckBox rememberCheckBox = dialogView.findViewById(R.id.rememberCheckBox);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedIp = sharedPreferences.getString(KEY_IP_ADDRESS, null);

        if (storedIp != null) {
            ipEditText.setText(storedIp);
            rememberCheckBox.setChecked(true);
        }

        new AlertDialog.Builder(this)
                .setTitle("Enter IP Address")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    ipAddress = ipEditText.getText().toString();
                    if (rememberCheckBox.isChecked()) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KEY_IP_ADDRESS, ipAddress);
                        editor.apply();
                    }
                    dispatchTakePictureIntent();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                .baseUrl("http://" + ipAddress + ":5000/")
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

                                String message = "Match found:\n" +
                                        "First Name: " + fName + "\n" +
                                        "Last Name: " + lName + "\n" +
                                        "Middle Name: " + mName + "\n" +
                                        "LRN: " + lrn;
                                showResultDialog("Success", message);

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

    private void queryStudentAndAddAttendanceRecord(String fName, String lName, String mName, String lrn, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students")
                .whereEqualTo("lrn", lrn)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String grade = document.getString("grade");
                        String section = document.getString("section");

                        addAttendanceRecord(fName, lName, mName, lrn, grade, section, imageUrl);
                    } else {
                        Log.w("Firestore", "No matching student found for LRN: " + lrn);
                        showResultDialog("Error", "Student record not found in Firestore.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error querying student data", e);
                    showResultDialog("Error", "Failed to retrieve student data.");
                });
    }

    private void addAttendanceRecord(String fName, String lName, String mName, String lrn, String grade, String section, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("FName", fName);
        attendanceData.put("LName", lName);
        attendanceData.put("MName", mName);
        attendanceData.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        attendanceData.put("grade", grade);
        attendanceData.put("image", imageUrl);
        attendanceData.put("remarks", "present");
        attendanceData.put("section", section);
        attendanceData.put("studentId", lrn);
        attendanceData.put("timeIn", new SimpleDateFormat("HH:mm").format(new Date()));

        db.collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Attendance record added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding attendance record", e);
                });
    }


    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    private interface ApiService {
        @Multipart
        @POST("upload_image")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
    }
}
