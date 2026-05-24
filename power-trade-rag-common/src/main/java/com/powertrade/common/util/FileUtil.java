package com.powertrade.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件工具类（不依赖 Spring）
 */
public class FileUtil {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/data/docs/";

    /**
     * 保存上传的文件
     * @param fileBytes 文件字节数组
     * @param originalFilename 原始文件名
     * @return 保存后的文件路径
     */
    public static String saveFile(byte[] fileBytes, String originalFilename) {
        try {
            String extension = getFileExtension(originalFilename);
            String newFileName = UUID.randomUUID().toString() + extension;
            
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            Path destPath = uploadDir.resolve(newFileName);
            Files.write(destPath, fileBytes);
            
            return destPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名（包含点）
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 获取文件类型（不含点）
     * @param fileName 文件名
     * @return 文件类型
     */
    public static String getFileType(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.startsWith(".") ? ext.substring(1) : ext;
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取上传目录
     * @return 上传目录路径
     */
    public static String getUploadDir() {
        return UPLOAD_DIR;
    }
}
