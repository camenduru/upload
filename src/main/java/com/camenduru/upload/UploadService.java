package com.camenduru.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadService {

    @Value("${camenduru.upload.s3.region}")
    private String camenduruWebS3Region;

    @Value("${camenduru.upload.s3.access}")
    private String camenduruWebS3Access;

    @Value("${camenduru.upload.s3.secret}")
    private String camenduruWebS3Secret;

    @Value("${camenduru.upload.s3.bucket}")
    private String camenduruWebS3Bucket;

    @Value("${camenduru.upload.s3.preview}")
    private String camenduruWebS3Preview;

    @Value("${camenduru.upload.s3.endpoint}")
    private String camenduruWebS3Endpoint;

    private S3Client s3Client;

    @PostConstruct
    public void initializeS3Client() {
        this.s3Client = S3Client.builder()
            .region(Region.of(camenduruWebS3Region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(camenduruWebS3Access, camenduruWebS3Secret)))
            .endpointOverride(URI.create(camenduruWebS3Endpoint))
            .build();
    }

    @PostMapping("/v1")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws URISyntaxException {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file provided.");
        }
        try {
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String fileType = filename.substring(filename.lastIndexOf('.') + 1);
            String uniqueFilename = UUID.randomUUID() + "." + fileType;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes)) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(camenduruWebS3Bucket)
                    .key(uniqueFilename)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

                s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(fileBytes));
                String url = String.format("%s/%s/%s", camenduruWebS3Preview, camenduruWebS3Bucket, uniqueFilename);
                return ResponseEntity.ok(url);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
        }
    }
}
