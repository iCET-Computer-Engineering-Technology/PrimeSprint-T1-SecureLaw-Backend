package com.primesprint.service;
import com.primesprint.dto.UploadResponse;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {

    private static final long MAX_SIZE = 20 * 1024 * 1024;

    public UploadResponse processFile(MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException("File too large");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new RuntimeException("Invalid file name");
        }

        // allow only these types
        if (!(filename.endsWith(".txt") || filename.endsWith(".csv") || filename.endsWith(".pdf") || filename.endsWith(".docx"))) {

            throw new RuntimeException("Unsupported file type");
        }

        // create secure temp file
        Path tempFile = Files.createTempFile("upload-", filename);

        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        // Apache Tika extracts text from any supported document
        Tika tika = new Tika();
        String extractedText = tika.parseToString(tempFile.toFile());

        // delete temp file immediately
        Files.deleteIfExists(tempFile);

        // preview first 500 characters
        String preview = extractedText.substring(0, Math.min(500, extractedText.length()));

        return new UploadResponse(UUID.randomUUID().toString(), preview);
    }
}