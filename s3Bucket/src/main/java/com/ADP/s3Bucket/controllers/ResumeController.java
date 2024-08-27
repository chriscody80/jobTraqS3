package com.ADP.s3Bucket.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ADP.s3Bucket.services.S3Service;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = s3Service.uploadFile(file); // Directly pass MultipartFile
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/download/{filename}")
public ResponseEntity<?> downloadResume(@PathVariable String filename) {
    // Determine the Downloads folder path
    String downloadsFolder = System.getProperty("user.home") + File.separator + "Downloads";
    Path tempFilePath = Paths.get(downloadsFolder, filename);

    System.out.println("Attempting to download file: " + filename + " to " + tempFilePath.toString());
    try {
        s3Service.downloadFile(filename, tempFilePath);

        if (Files.exists(tempFilePath)) {
            System.out.println("File downloaded successfully: " + tempFilePath.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .body(Files.readAllBytes(tempFilePath));
        } else {
            System.out.println("File not found on server: " + filename);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found on server.");
        }
    } catch (IOException e) {
        // Print the error with stack trace for better debugging
        System.out.println("Failed to download file: " + filename);
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to download file: " + e.getMessage());
    }
}
}
