package com.primesprint.service;

import com.primesprint.custom_annotation.Auditable;
import com.primesprint.dto.UploadResponse;
import com.primesprint.model.enums.ActionType;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    @Auditable(action = ActionType.DOCUMENT_UPLOADED)
    public UploadResponse processFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File too large");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        // allow only these types
        if (!(filename.endsWith(".txt") || filename.endsWith(".csv") || filename.endsWith(".pdf") || filename.endsWith(".docx"))) {

            throw new IllegalArgumentException("Unsupported file type");
        }

        try {
            Path tempFile = Files.createTempFile("upload-", filename);

            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            Tika tika = new Tika();
            String text = tika.parseToString(tempFile.toFile());

            text = text.replace("\\r\\n", "\n")
                    .replace("\\n", "\n")
                    .replace("\\r", "\n")
                    .replace("\\t", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            return new UploadResponse(UUID.randomUUID().toString(), text.substring(0, Math.min(25000000, text.length())));
            //20mb can hold maximum 25 million characters to prevent memory issues
        } catch (Exception e) {
            throw new UploadProcessingException("File processing failed", e);
        }
    }

    public static class UploadProcessingException extends RuntimeException {
        public UploadProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}