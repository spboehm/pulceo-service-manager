package dev.pulceo.prm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileManager {

    @Value("${pulceo.data.dir}")
    private String pulceoDataDir;

    Logger logger = LoggerFactory.getLogger(FileManager.class);

    public void saveAsJson(byte[] raw, String subfolder, String orchestrationUuid, String fileName) throws FileManagerServiceException {
        try {
            Path filePath = Path.of(this.pulceoDataDir, subfolder, orchestrationUuid, fileName);
            Files.write(filePath, raw); // Write the raw data to the file
            this.logger.info("Saved raw data to {}", filePath);
        } catch (IOException e) {
            this.logger.error("Failed to save raw data to {}/{}", subfolder, fileName, e);
            throw new FileManagerServiceException("Failed to save raw data", e);
        }
    }

    public boolean checkIfRequestedFileExists(Path requestedFilePath) {
        if (Files.exists(requestedFilePath)) {
            this.logger.info("File {} exists", requestedFilePath);
            return true;
        } else {
            this.logger.warn("File {} does not exist", requestedFilePath);
            return false;
        }
    }
}
