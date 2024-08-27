package com.ADP.s3Bucket.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String key = file.getOriginalFilename();
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build(), RequestBody.fromInputStream(inputStream, file.getSize()));
            return s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(key)).toExternalForm();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public void downloadFile(String objectKey, Path destinationPath) throws IOException {
        // Check if the file already exists
        if (Files.exists(destinationPath)) {
            // Generate a new filename with UUID
            String newFileName = getUniqueFileName(destinationPath);
            Path newDestinationPath = destinationPath.resolveSibling(newFileName);
            System.out.println("File already exists. Downloading to new file: " + newDestinationPath.toString());
            downloadFromS3(objectKey, newDestinationPath);
        } else {
            // Download file to the original destination path
            System.out.println("Downloading file from S3: " + objectKey + " to " + destinationPath.toString());
            downloadFromS3(objectKey, destinationPath);
        }
    }

    private void downloadFromS3(String objectKey, Path destinationPath) throws IOException {
        try {
            s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build(),
                ResponseTransformer.toFile(destinationPath)
            );
            System.out.println("File downloaded successfully: " + objectKey);
        } catch (Exception e) {
            System.out.println("Failed to download file from S3: " + objectKey);
            e.printStackTrace();
            throw new IOException("Failed to download file from S3", e);
        }
    }

    private String getUniqueFileName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String fileExtension = "";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = fileName.substring(lastDotIndex);
            fileName = fileName.substring(0, lastDotIndex);
        }

        String uniqueFileName = fileName + "_" + UUID.randomUUID().toString() + fileExtension;
        return filePath.resolveSibling(uniqueFileName).toString();
    }
}
