package com.doublehammerstudio.academeaseapp.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RegisterStudentActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;


    private Spinner sectionSpinner;
    private Button takePhotoButton, registerButton;
    private ImageView studentImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_student);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        firestore = FirebaseFirestore.getInstance();

        sectionSpinner = findViewById(R.id.section_spinner);
        takePhotoButton = findViewById(R.id.take_photo_button);
        registerButton = findViewById(R.id.register_button);
        studentImage = findViewById(R.id.student_image);


        takePhotoButton.setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imageCaptureLauncher.launch(intent);
        });

        fetchSections();
    }
    private ActivityResultLauncher<Intent> imageCaptureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

                    studentImage.setImageBitmap(imageBitmap);
                }
            });

    private void fetchSections() {
        Query sectionsQuery = firestore.collection("sections");
        sectionsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot querySnapshot, FirebaseFirestoreException
                    error) {
                if (error != null) {
                    Toast.makeText(RegisterStudentActivity.this, "Error fetching sections", Toast.LENGTH_SHORT).show();
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
            }
        });
    }

    private void populateSpinner(List<String> sectionsList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sectionsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(adapter);
    }
}