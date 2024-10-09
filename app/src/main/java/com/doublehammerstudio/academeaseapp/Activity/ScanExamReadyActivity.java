package com.doublehammerstudio.academeaseapp.Activity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.doublehammerstudio.academeaseapp.Interfaces.ApiService;
import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;

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

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class ScanExamReadyActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextView testNameTextView;
    private TextView selectedSetTextView;
    private TextView documentIdTextView;
    private Button showQuestionsButton;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Button capturePhotoButton;
    private String currentPhotoPath;
    private Button testHelloButton;
    private TextView testAnswerDocument;


    private String currentIpAddress;
    // Constants for SharedPreferences
    private static final String PREFS_NAME = "IPPrefs";
    private static final String KEY_IP_ADDRESS = "192.168.1.1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_exam_ready);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            testNameTextView = findViewById(R.id.testNameTextView);
            selectedSetTextView = findViewById(R.id.selectedSetTextView);
            documentIdTextView = findViewById(R.id.documentIdTextView);
            showQuestionsButton = findViewById(R.id.showQuestionsButton);
            capturePhotoButton = findViewById(R.id.capturePhotoButton);
            testAnswerDocument = findViewById(R.id.testAnswerDocument);

            String testName = getIntent().getStringExtra("testName");
            String selectedSet = getIntent().getStringExtra("selectedSet");
            String documentId = getIntent().getStringExtra("documentId");

            testNameTextView.setText("Test: " + testName);
            selectedSetTextView.setText("Selected Set: " + selectedSet);
            documentIdTextView.setText("Document ID: " + documentId);

            fetchAnswersOnActivityLoad(documentId, selectedSet);

            showQuestionsButton.setOnClickListener(v1 -> {
                fetchQuestionsAndShowDialog(documentId, selectedSet);
            });

            capturePhotoButton.setOnClickListener(v1 -> {


                showIpInputDialog();


//                dispatchTakePictureIntent();
            });
//            testHelloButton = findViewById(R.id.testHelloButton);
//            testHelloButton.setOnClickListener(v1 -> {
//                testHelloEndpoint();
//            });
            return insets;
        });
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
                    String ipAddress = ipEditText.getText().toString();
                    boolean rememberIp = rememberCheckBox.isChecked();

                    if (rememberIp) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KEY_IP_ADDRESS, ipAddress);
                        editor.apply();
                    }

                    dispatchTakePictureIntent(ipAddress);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void fetchQuestionsAndShowDialog(String documentId, String selectedSet) {
        db.collection("tests").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
            Log.d("Firestore", "Fetched document: " + documentSnapshot.getId());
            if (documentSnapshot.exists()) {
                Map<String, Object> questionsField = (Map<String, Object>) documentSnapshot.get("questions");
                if (questionsField != null) {
                    String setKey = selectedSet.equals("Set A") ? "A" : "B";
                    Log.d("Firestore", "Selected set key: " + setKey);
                    ArrayList<Map<String, Object>> questionsList = (ArrayList<Map<String, Object>>) questionsField.get(setKey);

                    if (questionsList != null) {
                        ArrayList<String> questionDetails = new ArrayList<>();
                        ArrayList<String> correctAnswersList = new ArrayList<>();
                        int counter = 1;

                        for (Map<String, Object> questionMap : questionsList) {
                            String questionText = (String) questionMap.get("question");
                            Log.d("Firestore", "Question fetched: " + questionText);

                            ArrayList<Map<String, Object>> choices = (ArrayList<Map<String, Object>>) questionMap.get("choices");
                            StringBuilder choicesText = new StringBuilder();
                            for (Map<String, Object> choice : choices) {
                                choicesText.append(choice.get("id"))
                                        .append(": ")
                                        .append(choice.get("text"))
                                        .append("\n");
                            }

                            String correctAnswerStr = (String) questionMap.get("correctAnswer");
                            Log.d("Firestore", "Correct answer fetched: " + correctAnswerStr);
                            correctAnswersList.add("Question " + counter + ": " + correctAnswerStr);

                            String questionDetail = "Question " + counter + ": " + questionText + "\n" +
                                    "Choices:\n" + choicesText.toString() +
                                    "Correct Answer: " + correctAnswerStr + "\n";
                            questionDetails.add(questionDetail);
                            counter++;
                        }

                        if (!questionDetails.isEmpty()) {
                            showQuestionsDialog(questionDetails);
                        } else {
                            Log.e("Firestore", "No questions found for this set.");
                            Toast.makeText(this, "No questions found for this set.", Toast.LENGTH_SHORT).show();
                        }


                    } else {
                        Log.e("Firestore", "Selected set not found in the document.");
                        Toast.makeText(this, "Selected set not found in the document.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("Firestore", "Questions field not found in the document.");
                    Toast.makeText(this, "Questions field not found in the document.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("Firestore", "Test data not found.");
                Toast.makeText(this, "Test data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error fetching questions: " + e.getMessage());
            Toast.makeText(this, "Error fetching questions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchAnswersOnActivityLoad(String documentId, String selectedSet) {
        db.collection("tests").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> questionsField = (Map<String, Object>) documentSnapshot.get("questions");
                if (questionsField != null) {
                    String setKey = selectedSet.equals("Set A") ? "A" : "B";
                    ArrayList<Map<String, Object>> questionsList = (ArrayList<Map<String, Object>>) questionsField.get(setKey);

                    if (questionsList != null) {
                        ArrayList<Integer> answersList = new ArrayList<>();  // List to hold correct answers as integers

                        for (Map<String, Object> questionMap : questionsList) {
                            String correctAnswerStr = (String) questionMap.get("correctAnswer");
                            int correctAnswer = convertAnswerToInt(correctAnswerStr);  // Convert answer to 0,1,2,3
                            answersList.add(correctAnswer);  // Add correct answer to the list
                        }

                        updateCorrectAnswersInTextView(answersList);
                    } else {
                        Log.e("Firestore", "Selected set not found in the document.");
                        Toast.makeText(this, "Selected set not found in the document.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("Firestore", "Questions field not found in the document.");
                    Toast.makeText(this, "Questions field not found in the document.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("Firestore", "Test data not found.");
                Toast.makeText(this, "Test data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error fetching questions: " + e.getMessage());
            Toast.makeText(this, "Error fetching questions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCorrectAnswersInTextView(ArrayList<Integer> correctAnswersList) {

        StringBuilder correctAnswersText = new StringBuilder("Answers: [");

        for (int i = 0; i < correctAnswersList.size(); i++) {
            correctAnswersText.append(correctAnswersList.get(i));
            if (i != correctAnswersList.size() - 1) {
                correctAnswersText.append(", ");
            }
        }
        correctAnswersText.append("]");

        testAnswerDocument.setText(correctAnswersText.toString());
    }


    private int convertAnswerToInt(String correctAnswer) {
        switch (correctAnswer) {
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            default: throw new IllegalArgumentException("Invalid answer: " + correctAnswer);
        }
    }

    private void showQuestionsDialog(ArrayList<String> questionDetails) {
        CharSequence[] questionsArray = questionDetails.toArray(new CharSequence[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Questions, Choices, and Answers")
                .setItems(questionsArray, null)
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }



    private void dispatchTakePictureIntent(String ipAddress) {
        currentIpAddress = ipAddress;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CameraCapture", "Error occurred while creating the File: " + ex.getMessage());
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.doublehammerstudio.academeaseapp.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
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
            File originalFile = new File(currentPhotoPath);

            ArrayList<Integer> answersList = extractAnswersFromTextView(testAnswerDocument);

            uploadImageAndAnswers(originalFile, answersList);
            //uploadTestImage(originalFile);
        }
    }

    private ArrayList<Integer> extractAnswersFromTextView(TextView textView) {
        ArrayList<Integer> answersList = new ArrayList<>();
        String text = textView.getText().toString();  // Extract the text
        text = text.replace("Answers: [", "").replace("]", "").trim();  // Clean up the string
        String[] answersArray = text.split(", ");

        for (String answer : answersArray) {
            try {
                answersList.add(Integer.parseInt(answer));
            } catch (NumberFormatException e) {
                Log.e("ExtractAnswers", "Error parsing answer: " + answer);
            }
        }
        return answersList;
    }


    private void uploadImageAndAnswers(File imageFile, ArrayList<Integer> answers) {
        Retrofit retrofit = createRetrofit();
        ApiService uploadService = retrofit.create(ApiService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        Gson gson = new Gson();
        String answersJson = gson.toJson(answers);
        RequestBody answersBody = RequestBody.create(MediaType.parse("text/plain"), answersJson);

        Call<ResponseBody> call = uploadService.uploadImageAndAnswers(body, answersBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();

                        JSONObject jsonResponse = new JSONObject(responseBody);

                        String setVal = jsonResponse.getString("set_val");
                        String digitText = jsonResponse.getString("digit_text").trim().replace(" ", ""); 
                        int score = jsonResponse.getInt("score");
                        String rating = jsonResponse.getString("rating");

                        if (digitText.length() != 12) {
                            showResultDialog("Error", "OMR API Failed to read the LRN, please try to capture again");
                            return;
                        }

                        String resultMessage = "Exam Set: " + setVal + "\n"
                                + "LRN: " + digitText + "\n"
                                + "Score: " + score + "\n"
                                + "Rating: " + rating;

                        Log.d("API Response", "Response: " + resultMessage);

                        showResultDialog("Success", resultMessage);

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        showResultDialog("Error", "Error processing response: " + e.getMessage());
                    }

                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonError = new JSONObject(errorBody);
                        String errorMessage = jsonError.getString("error");

                        Log.e("API Response", "Failed: " + errorMessage);
                        showResultDialog("Error", "Upload failed: " + errorMessage);

                    } catch (Exception e) {
                        Log.e("API Error", "Error: " + e.getMessage());
                        showResultDialog("Error", "Upload failed with code: " + response.code());
                    }
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API Error", "Error: " + t.getMessage());
                showResultDialog("Error", "Upload failed: " + t.getMessage());
            }
        });
    }



    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void testHelloEndpoint() {
        Retrofit retrofit = createRetrofit();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.testHello();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Toast.makeText(ScanExamReadyActivity.this, "API is Accessible: Response: " + responseBody, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ScanExamReadyActivity.this, "Error reading response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ScanExamReadyActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ScanExamReadyActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private Retrofit createRetrofit() {

        return new Retrofit.Builder()
                .baseUrl("http://" + currentIpAddress + ":5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }




    private void uploadTestImage(File imageFile) {
        Retrofit retrofit = createRetrofit();
        ApiService uploadService = retrofit.create(ApiService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        Call<ResponseBody> call = uploadService.uploadImageTest(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("API Response", "Response: " + responseBody);
                        Toast.makeText(ScanExamReadyActivity.this, "Image upload successful!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ScanExamReadyActivity.this, "Error processing response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API Response", "Upload failed with code: " + response.code());
                    Toast.makeText(ScanExamReadyActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API Error", "Error: " + t.getMessage());
                Toast.makeText(ScanExamReadyActivity.this, "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
