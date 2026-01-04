package br.com.groupsoftware.grouppay.extratoremail.util.file;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe utilitária para manipulação de arquivos e diretórios.
 * <p>
 * Esta classe fornece métodos para criar diretórios, mover, copiar, excluir arquivos e manipular o conteúdo de arquivos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@UtilityClass
public class FileUtils {

    public static void createDirectoryIfNotExists(Path directory) throws IOException {
        if (Files.notExists(directory)) {
            Files.createDirectories(directory);
        }
    }

    public static void writeToFile(InputStream inputStream, Path targetFile) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192]; // Buffer para escrita eficiente
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public void moveFile(Path source, Path destination) throws IOException {
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public void copyFile(Path source, Path destination) throws IOException {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean deleteFile(File file) {
        return file.exists() && file.delete();
    }

    public List<File> listPdfFiles(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    public void writeToFile(String content, File outputFile) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(content.getBytes());
        }
    }

    public String readFileAsString(File inputFile) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            return new String(inputStream.readAllBytes());
        }
    }
}

