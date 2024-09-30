package com.doublehammerstudio.academeaseapp.Models;

public class TestItem {
    private String testName;
    private String testDate;
    private String teacherName;
    private String documentId;  // Add documentId

    public TestItem(String testName, String testDate, String teacherName, String documentId) {
        this.testName = testName;
        this.testDate = testDate;
        this.teacherName = teacherName;
        this.documentId = documentId;  // Store documentId
    }

    public String getTestName() {
        return testName;
    }

    public String getTestDate() {
        return testDate;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getDocumentId() {
        return documentId;  // Getter for documentId
    }
}
