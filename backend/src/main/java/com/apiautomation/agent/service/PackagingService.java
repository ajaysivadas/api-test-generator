package com.apiautomation.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PackagingService {

    @Value("${agent.output.dir}")
    private String outputBaseDir;

    public byte[] packageAsZip(String generationId) throws IOException {
        Path sourceDir = Path.of(outputBaseDir, generationId);
        if (!Files.exists(sourceDir)) {
            throw new FileNotFoundException("Generation output not found: " + generationId);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String rootFolder = "generated-framework/";

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = sourceDir.relativize(file).toString();
                    // Normalize to forward slashes for ZIP compatibility
                    relativePath = relativePath.replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(rootFolder + relativePath));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDir)) {
                        String relativePath = sourceDir.relativize(dir).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(rootFolder + relativePath + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    public byte[] packageAsZip(String generationId, String rootFolder) throws IOException {
        Path sourceDir = Path.of(outputBaseDir, generationId);
        if (!Files.exists(sourceDir)) {
            throw new FileNotFoundException("Generation output not found: " + generationId);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String prefix = (rootFolder == null || rootFolder.isBlank()) ? "" : rootFolder;

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = sourceDir.relativize(file).toString();
                    relativePath = relativePath.replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(prefix + relativePath));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDir)) {
                        String relativePath = sourceDir.relativize(dir).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(prefix + relativePath + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return baos.toByteArray();
    }

    public void cleanup(String generationId) throws IOException {
        Path dir = Path.of(outputBaseDir, generationId);
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
