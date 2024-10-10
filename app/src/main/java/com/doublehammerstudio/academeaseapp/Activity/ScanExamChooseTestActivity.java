package com.doublehammerstudio.academeaseapp.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doublehammerstudio.academeaseapp.Adapters.TestAdapter;
import com.doublehammerstudio.academeaseapp.Models.TestItem;
import com.doublehammerstudio.academeaseapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ScanExamChooseTestActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TestAdapter testAdapter;
    private ArrayList<TestItem> testList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_exam);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show the back button
        getSupportActionBar().setHomeButtonEnabled(true); // Enable the button

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        testAdapter = new TestAdapter(testList);
        recyclerView.setAdapter(testAdapter);

        fetchTests();

        testAdapter.setOnItemClickListener(testItem -> showSetSelectionDialog(testItem));
    }

    private void fetchTests() {
        db.collection("tests").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String testName = document.getString("name");
                    String testDate = document.getString("date");
                    String createdBy = document.getString("createdBy");
                    String documentId = document.getId();  // Get document ID

                    fetchTeacherInfo(createdBy, testName, testDate, documentId);
                }
            } else {
                Log.e("Firestore", "Error getting documents.", task.getException());
            }
        });
    }

    private void fetchTeacherInfo(String teacherId, String testName, String testDate, String documentId) {
        db.collection("teachers-info").document(teacherId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("firstName");
                String lastName = documentSnapshot.getString("lastName");
                String fullName = firstName + " " + lastName;

                // Add the test item with documentId to the list
                testList.add(new TestItem(testName, testDate, fullName, documentId));
                testAdapter.notifyDataSetChanged();
            }
        });
    }


    private void showSetSelectionDialog(TestItem testItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Set")
                .setItems(new String[]{"Set A", "Set B"}, (dialogInterface, i) -> {
                    String selectedSet = (i == 0) ? "Set A" : "Set B";
                    saveTestConfiguration(testItem, selectedSet);
                });
        builder.create().show();
    }

    private void saveTestConfiguration(TestItem testItem, String selectedSet) {
        Log.d("TestConfig", "Selected Test: " + testItem.getTestName() + ", Selected Set: " + selectedSet + ", Document ID: " + testItem.getDocumentId());

        Intent intent = new Intent(ScanExamChooseTestActivity.this, ScanExamReadyActivity.class);
        intent.putExtra("testName", testItem.getTestName());
        intent.putExtra("selectedSet", selectedSet);
        intent.putExtra("documentId", testItem.getDocumentId());
        startActivity(intent);
    }

}