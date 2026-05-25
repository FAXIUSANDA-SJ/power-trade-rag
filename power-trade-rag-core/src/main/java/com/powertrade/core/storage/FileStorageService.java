package com.powertrade.core.storage;

public interface FileStorageService {

    StoredFile save(byte[] fileBytes, String originalFilename);

    byte[] read(String accessPath);

    boolean delete(String accessPath);
}
