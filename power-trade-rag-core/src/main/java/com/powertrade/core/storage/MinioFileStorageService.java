package com.powertrade.core.storage;

import com.powertrade.common.util.FileUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MinIO 存储预留实现。
 * 当前阶段先复用本地落盘，统一接口与配置入口，后续再替换成真实 MinIO SDK。
 */
@Service
@ConditionalOnProperty(name = "rag.minio.enabled", havingValue = "true")
public class MinioFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageService.class);

    @Value("${rag.minio.bucket-name:power-trade-rag}")
    private String bucketName;

    @Value("${rag.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${rag.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${rag.minio.secret-key:minioadmin}")
    private String secretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("已创建 MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化 MinIO bucket 失败", e);
        }
    }

    @Override
    public StoredFile save(byte[] fileBytes, String originalFilename) {
        try {
            String extension = FileUtil.getFileExtension(originalFilename);
            String objectKey = "docs/" + UUID.randomUUID().toString() + extension;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            StoredFile storedFile = new StoredFile();
            storedFile.setStorageType("minio");
            storedFile.setObjectKey(objectKey);
            storedFile.setAccessPath("minio://" + bucketName + "/" + objectKey);
            storedFile.setFileSize((long) fileBytes.length);
            return storedFile;
        } catch (Exception e) {
            throw new RuntimeException("上传文件到 MinIO 失败", e);
        }
    }

    @Override
    public byte[] read(String accessPath) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(extractObjectKey(accessPath))
                        .build());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 读取文件失败", e);
        }
    }

    @Override
    public boolean delete(String accessPath) {
        try {
            String objectKey = extractObjectKey(accessPath);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 删除文件失败", e);
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    private String extractObjectKey(String accessPath) {
        String prefix = "minio://" + bucketName + "/";
        if (accessPath != null && accessPath.startsWith(prefix)) {
            return accessPath.substring(prefix.length());
        }
        return accessPath;
    }
}
