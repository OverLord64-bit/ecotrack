package com.ecotrack.report.storage;

import com.ecotrack.report.common.InvalidInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Stores report photos on a volume (the service itself stays stateless: the volume holds the files). */
@Service
public class PhotoStorage {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final Path directory;

    public PhotoStorage(@Value("${ecotrack.uploads.dir:uploads}") String directory) {
        this.directory = Path.of(directory);
    }

    public String store(UUID reportId, MultipartFile file) {
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new InvalidInputException("photo must be a JPEG, PNG or WebP image");
        }
        if (file.isEmpty()) {
            throw new InvalidInputException("photo is empty");
        }
        String filename = reportId + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return filename;
    }

    public Resource load(String filename) {
        return new FileSystemResource(directory.resolve(filename));
    }

    public static String contentType(String filename) {
        return filename.endsWith(".png") ? "image/png" : filename.endsWith(".webp") ? "image/webp" : "image/jpeg";
    }
}
