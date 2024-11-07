package com.doublehammerstudio.academeaseapp.Activity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import java.util.Stack;
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

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private String currentIpAddress;
    // Constants for SharedPreferences
    private static final String PREFS_NAME = "IPPrefs";
    private static final String KEY_IP_ADDRESS = "192.168.1.1";

    // Hardcoded IP address for automatic connection
    private static final String API_BASE_URL = "http://192.168.0.101:5000/";

    private String baseUrl = "http://192.168.0.101:5000/"; // Default URL in case Firebase retrieval fails

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
            fetchBaseUrlFromFirestore();
            showQuestionsButton.setOnClickListener(v1 -> {
                fetchQuestionsAndShowDialog(documentId, selectedSet);
            });

            capturePhotoButton.setOnClickListener(v1 -> {
                dispatchTakePictureIntent();
            });
            return insets;
        });
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
                            String correctAnswerText = "";

                            // Get the correct answer as a String (assuming it's stored as a letter like "A", "B", etc.)
                            String correctAnswerLetter = (String) questionMap.get("correctAnswer");

                            for (Map<String, Object> choice : choices) {
                                Long choiceId = (Long) choice.get("id");
                                int idAsInt = choiceId.intValue();

                                // Convert the ID to a letter (A, B, C, D)
                                String letter = convertNumberToLetter(idAsInt);
                                String choiceText = (String) choice.get("text");

                                // Append the letter and choice text to choicesText
                                choicesText.append(letter)
                                        .append(": ")
                                        .append(choiceText)
                                        .append("\n");

                                // Check if this choice letter matches the correct answer letter
                                if (letter.equals(correctAnswerLetter)) {
                                    correctAnswerText = letter + ": " + choiceText; // Store letter + correct answer text
                                }
                            }

                            String questionDetail = "Question " + counter + ": " + questionText + "\n" +
                                    "Choices:\n" + choicesText.toString() +
                                    "Correct Answer: " + correctAnswerText + "\n";

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

    private String convertNumberToLetter(int number) {
        switch (number) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return String.valueOf(number);
        }
    }


    private void fetchQuestionsAndShowDialogtry(
            String documentId, String selectedSet, String shadedResponseLetters,
            String lrn, String name, String section) {

        db.collection("tests").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
            Log.d("Firestore", "Fetched document: " + documentSnapshot.getId());
            if (documentSnapshot.exists()) {

                // Retrieve test name and creator
                String testName = documentSnapshot.getString("name");
                String createdBy = documentSnapshot.getString("createdBy");

                Map<String, Object> questionsField = (Map<String, Object>) documentSnapshot.get("questions");
                if (questionsField != null) {
                    String setKey = selectedSet.equals("Set A") ? "A" : "B";
                    Log.d("Firestore", "Selected set key: " + setKey);
                    ArrayList<Map<String, Object>> questionsList =
                            (ArrayList<Map<String, Object>>) questionsField.get(setKey);

                    if (questionsList != null && shadedResponseLetters.length() >= questionsList.size()) {
                        ArrayList<String> comparisonResults = new ArrayList<>();
                        ArrayList<Map<String, Object>> questionResults = new ArrayList<>();
                        int correct = 0;
                        int incorrect = 0;
                        int counter = 1;

                        // Loop through questions and evaluate answers
                        for (int i = 0; i < questionsList.size(); i++) {
                            Map<String, Object> questionMap = questionsList.get(i);
                            String questionText = (String) questionMap.get("question");
                            String correctAnswerLetter = (String) questionMap.get("correctAnswer");
                            String correctAnswerText = "";

                            ArrayList<Map<String, Object>> choices =
                                    (ArrayList<Map<String, Object>>) questionMap.get("choices");
                            StringBuilder choicesText = new StringBuilder();

                            // Process each choice to build text and find correct answer
                            for (Map<String, Object> choice : choices) {
                                Long choiceId = (Long) choice.get("id");
                                int idAsInt = choiceId.intValue();
                                String letter = convertNumberToLetter(idAsInt);
                                String choiceText = (String) choice.get("text");

                                choicesText.append(letter).append(": ").append(choiceText).append("<br>");
                                if (letter.equals(correctAnswerLetter)) {
                                    correctAnswerText = letter + ": " + choiceText;
                                }
                            }

                            // Get student's response
                            char userResponseLetter = shadedResponseLetters.charAt(i);
                            String userResponseText = "";

                            for (Map<String, Object> choice : choices) {
                                Long choiceId = (Long) choice.get("id");
                                int idAsInt = choiceId.intValue();
                                String letter = convertNumberToLetter(idAsInt);
                                String choiceText = (String) choice.get("text");

                                if (letter.equals(String.valueOf(userResponseLetter))) {
                                    userResponseText = letter + ": " + choiceText;
                                    break;
                                }
                            }

                            // Create result string for dialog display
                            String result = "Question " + counter + ": " + questionText + "<br>"
                                    + "Choices:<br>" + choicesText.toString() + "<br>"
                                    + "Correct Answer: " + correctAnswerText + "<br>"
                                    + "Student Response: " + userResponseText + "<br>";

                            // Check if the answer is correct
                            boolean isCorrect = correctAnswerLetter.equals(String.valueOf(userResponseLetter));
                            if (isCorrect) {
                                result += "<font color='green'>Result: Correct</font><br>";
                                correct++;
                            } else {
                                result += "<font color='red'>Result: Incorrect</font><br>";
                                incorrect++;
                            }

                            // Store the result for Firestore
                            Map<String, Object> questionResult = new HashMap<>();
                            questionResult.put("question_number", counter);
                            questionResult.put("question_text", questionText);
                            questionResult.put("is_correct", isCorrect);
                            questionResults.add(questionResult);

                            comparisonResults.add(result);
                            counter++;
                        }

                        if (!comparisonResults.isEmpty()) {
                            // Display the results in a dialog
                            showQuestionsDialog1(comparisonResults);

                            // Prepare Firestore data structure
                            Map<String, Object> scoreData = new HashMap<>();

                            // 'scores' object with question results and totals
                            Map<String, Object> scores = new HashMap<>();
                            scores.put("question_results", questionResults);
                            scores.put("total_correct", correct);
                            scores.put("total_incorrect", incorrect);

                            // Student info with nested details (lrn, name, section)
                            Map<String, Object> studentInfo = new HashMap<>();
                            studentInfo.put("lrn", lrn);
                            studentInfo.put("name", name);
                            studentInfo.put("section", section);

                            // Add top-level fields outside of 'scores'
                            scoreData.put("created_by", createdBy);
                            scoreData.put("student_info", studentInfo);  // Nested student info
                            scoreData.put("test_id", documentId);          // Outside 'scores'
                            scoreData.put("test_name", testName);          // Outside 'scores'
                            scoreData.put("scores", scores);               // 'scores' object

                            // Write the data to Firestore
                            String scoreDocumentId = db.collection("scores").document().getId();
                            db.collection("scores").document(scoreDocumentId)
                                    .set(scoreData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Score data successfully written!");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error writing score data", e);
                                    });
                        } else {
                            Log.e("Firestore", "No questions found for this set.");
                            Toast.makeText(this, "No questions found for this set.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Firestore", "Selected set not found or responses do not match questions.");
                        Toast.makeText(this, "Selected set not found or responses do not match questions.", Toast.LENGTH_SHORT).show();
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
    private Stack<AlertDialog> dialogStack = new Stack<>();

    private void showQuestionsDialog1(ArrayList<String> comparisonResults) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Results");

        StringBuilder htmlResults = new StringBuilder();
        for (String result : comparisonResults) {
            htmlResults.append(result).append("<br>");
        }

        builder.setMessage(Html.fromHtml(htmlResults.toString(), Html.FROM_HTML_MODE_LEGACY));

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();

        builder.setNegativeButton("Back", (dialog, which) -> {
            dialog.dismiss();
            dialogStack.pop(); // Remove current dialog from the stack
            if (!dialogStack.isEmpty()) {
                dialogStack.peek().show(); // Show the previous dialog if it exists
            }
        });
    }

    private void showResultDialog1(String title, String message, String shadedResponseLetters, String lrn, String name, String section) {
        String selectedSet = getIntent().getStringExtra("selectedSet");
        String documentId = getIntent().getStringExtra("documentId");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("See Answer", (dialogInterface, which) -> {
                    // Pass shadedResponseLetters to fetchQuestionsAndShowDialogtry
                    fetchQuestionsAndShowDialogtry(documentId, selectedSet, shadedResponseLetters, lrn, name, section);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialogStack.push(dialog); // Add current dialog to the stack
        dialog.show();
    }




    private void dispatchTakePictureIntent() {

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
                        String shaded_response = jsonResponse.getString("shaded_answers");

                        // Convert the numeric response to letter format
                        shaded_response = shaded_response.replaceAll("[\\[\\],\\s]", "");
                        StringBuilder shadedResponseLetters = new StringBuilder();
                        for (int i = 0; i < shaded_response.length(); i++) {
                            char numberChar = shaded_response.charAt(i);
                            int number = Character.getNumericValue(numberChar);
                            if (number >= 0 && number < 26) {
                                char letter = (char) ('A' + number);
                                shadedResponseLetters.append(letter);
                            }
                        }

                        String finalShadedResponse = shadedResponseLetters.toString();
                        if (digitText.length() != 12) {
                            showResultDialog("Error", "OMR API Failed to read the LRN, please try to capture again");
                            return;
                        }

                        // Firestore query to match LRN
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("students")
                                .whereEqualTo("lrn", digitText)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            String studentName = document.getString("FName") + " " +
                                                    document.getString("MName") + " " +
                                                    document.getString("LName");
                                            String section = document.getString("section");  // Retrieve the section

                                            // Build the result message
                                            String resultMessage = "Student Name: " + studentName + "\n"
                                                    + "Section: " + section + "\n"  // Include section
                                                    + "LRN: " + digitText + "\n"
                                                    + "Score: " + score + "\n"
                                                    + "Rating: " + rating + "\n";

                                            showResultDialog1("Success", resultMessage, finalShadedResponse, digitText, studentName, section);

                                            break;  // Exit after first match if only one match is expected
                                        }
                                    } else {
                                        showResultDialog("Error", "No student found with the LRN: " + digitText);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore Error", "Error fetching student details: " + e.getMessage());
                                    showResultDialog("Error", "Error fetching student details: " + e.getMessage());
                                });

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



    private void fetchBaseUrlFromFirestore() {
        DocumentReference docRef = firestore.collection("API").document("documentID");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String apiUrl = document.getString("omr");
                    if (apiUrl != null) {
                        baseUrl = apiUrl; // Set the base URL from Firestore
                    }
                } else {
                    System.out.println("No such document! Using default URL.");
                }
            } else {
                System.err.println("Failed to fetch document: " + task.getException());
            }
        });
    }

    private Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(baseUrl) // Use the dynamically retrieved or default base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public Retrofit getRetrofit() {
        return createRetrofit();
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