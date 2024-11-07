package com.doublehammerstudio.academeaseapp.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterStudentActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private EditText lrnEditText, lastNameEditText, firstNameEditText, middleNameEditText,
            genderEditText, birthdateEditText, ageEditText, addressEditText, contactEditText,
            emailEditText, gradeEditText, acadYearEditText;
    private Spinner sectionSpinner;
    private Button registerButton, captureImageButton;
    private ImageView studentImage;

    private Uri capturedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize EditTexts and Spinner
        lrnEditText = findViewById(R.id.lrn_edittext);
        lastNameEditText = findViewById(R.id.last_name_edittext);
        firstNameEditText = findViewById(R.id.first_name_edittext);
        middleNameEditText = findViewById(R.id.middle_name_edittext);
        genderEditText = findViewById(R.id.sex_edittext);
        birthdateEditText = findViewById(R.id.birthdate_edittext);
        ageEditText = findViewById(R.id.age_edittext);
        addressEditText = findViewById(R.id.address_edittext);
        contactEditText = findViewById(R.id.contact_edittext);
        emailEditText = findViewById(R.id.email_edittext);
        gradeEditText = findViewById(R.id.grade_edittext);
        acadYearEditText = findViewById(R.id.acad_edittext);
        sectionSpinner = findViewById(R.id.section_spinner);

        registerButton = findViewById(R.id.register_button);
        captureImageButton = findViewById(R.id.take_photo_button);
        studentImage = findViewById(R.id.student_image);

        captureImageButton.setOnClickListener(view -> openCamera());
        registerButton.setOnClickListener(view -> registerStudent());

        fetchSections();
    }

    private void openCamera() {
        File imageFile = new File(getExternalFilesDir("Pictures"), "student_image.png");
        capturedImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            imageCaptureLauncher.launch(takePictureIntent);
        }
    }


    private final ActivityResultLauncher<Intent> imageCaptureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Set the captured image URI to the ImageView
                    studentImage.setImageURI(capturedImageUri);
                } else {
                    Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show();
                }
            });


    private void registerStudent() {
        // Gather data from EditText fields
        String lrn = lrnEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString();
        String middleName = middleNameEditText.getText().toString();
        String gender = genderEditText.getText().toString();
        String birthdate = birthdateEditText.getText().toString();
        String age = ageEditText.getText().toString();
        String address = addressEditText.getText().toString();
        String contactNumber = contactEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String grade = gradeEditText.getText().toString();
        String acadYear = acadYearEditText.getText().toString();
        String section = sectionSpinner.getSelectedItem().toString();

        // Prepare data for Firestore
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("lrn", lrn);
        studentData.put("LName", lastName);
        studentData.put("FName", firstName);
        studentData.put("MName", middleName);
        studentData.put("gender", gender);
        studentData.put("dateOfBirth", birthdate);
        studentData.put("age", age);
        studentData.put("address", address);
        studentData.put("contactNumber", contactNumber);
        studentData.put("emailAddress", email);
        studentData.put("grade", grade);
        studentData.put("acadYear", acadYear);
        studentData.put("section", section);

        DocumentReference newStudentRef = firestore.collection("students").document();
        newStudentRef.set(studentData).addOnSuccessListener(unused -> {
            if (capturedImageUri != null) {
                uploadImage(newStudentRef.getId(), capturedImageUri);
            } else {
                Toast.makeText(this, "Student registered without photo", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error registering student", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchSections() {
        Query sectionsQuery = firestore.collection("sections");
        sectionsQuery.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error fetching sections", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> sectionsList = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String sectionName = document.getString("section");
                if (sectionName != null) {
                    sectionsList.add(sectionName);
                }
            }
            populateSpinner(sectionsList);
        });
    }

    private void populateSpinner(List<String> sectionsList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sectionsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(adapter);
    }

    private void uploadImage(String documentId, Uri imageUri) {
        StorageReference imageRef = storage.getReference().child("students/" + documentId + "/profile.png");
        imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                firestore.collection("students").document(documentId).update("image", uri.toString());
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        });
    }
}
