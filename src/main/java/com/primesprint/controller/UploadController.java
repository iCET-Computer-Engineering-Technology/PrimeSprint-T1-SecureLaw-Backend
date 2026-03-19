package com.primesprint.controller;
import com.primesprint.dto.UploadResponse;
import com.primesprint.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public UploadResponse uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        return fileService.processFile(file);
    }
    @GetMapping("/IsSizeMax")
    public boolean isSizeMax(@RequestParam("file") MultipartFile file) {
        return file.getSize() > 5 * 1024 * 1024;
    }
}