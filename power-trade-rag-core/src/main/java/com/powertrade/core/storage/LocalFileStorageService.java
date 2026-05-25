package com.powertrade.core.storage;

import com.powertrade.common.util.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rag.minio.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Override
    public StoredFile save(byte[] fileBytes, String originalFilename) {
        String path = FileUtil.saveFile(fileBytes, originalFilename);
        StoredFile storedFile = new StoredFile();
        storedFile.setStorageType("local");
        storedFile.setObjectKey(path);
        storedFile.setAccessPath(path);
        storedFile.setFileSize((long) fileBytes.length);
        return storedFile;
    }

    @Override
    public byte[] read(String accessPath) {
        try {
            return Files.readAllBytes(Paths.get(accessPath));
        } catch (IOException e) {
            throw new RuntimeException("读取本地文件失败", e);
        }
    }

    @Override
    public boolean delete(String accessPath) {
        return FileUtil.deleteFile(accessPath);
    }
}
