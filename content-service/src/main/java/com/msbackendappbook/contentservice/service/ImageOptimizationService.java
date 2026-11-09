package com.msbackendappbook.contentservice.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ImageOptimizationService {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 1200;
    private static final double QUALITY = 0.8;
    private static final String OUTPUT_FORMAT = "jpg";

    public MultipartFile optimizeImage(MultipartFile file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Redimensionner et compresser l'image
        Thumbnails.of(file.getInputStream())
                .size(MAX_WIDTH, MAX_HEIGHT)
                .outputFormat(OUTPUT_FORMAT)
                .outputQuality(QUALITY)
                .toOutputStream(outputStream);

        byte[] bytes = outputStream.toByteArray();

        return new OptimizedMultipartFile(
                bytes,
                file.getName(),
                "image/jpeg",
                bytes.length
        );
    }

    private static class OptimizedMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;
        private final long size;

        public OptimizedMultipartFile(byte[] content, String name, String contentType, long size) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
            this.size = size;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            // Not implemented
        }
    }
}