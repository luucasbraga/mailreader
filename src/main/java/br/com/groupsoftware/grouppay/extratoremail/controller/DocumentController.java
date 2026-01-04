package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.PdfDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.PythonExtractor;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Controlador REST para processamento e extração de dados de documentos PDF.
 * <p>
 * Fornece endpoints para processar arquivos PDF de forma individual ou em lote, além de um método
 * de extração de texto usando Python. Os resultados são retornados como objetos {@link Expense} ou
 * como strings com o texto extraído.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final ServiceFacade service;
    private final PythonExtractor pythonExtractor;

    @GetMapping("/process-pdf")
    public ResponseEntity<Expense> processPdf(@RequestBody PdfDTO dto) throws MailReaderException {
        Path path = Path.of(dto.getFilePathName());
        return ResponseEntity.ok(service.document.processPdfFile(path, dto.getFileNameOrSuffix()));
    }

    @GetMapping("/process-pdfs")
    public ResponseEntity<List<Expense>> processPdfs(@RequestBody PdfDTO dto) throws MailReaderException {
        List<Expense> list = new ArrayList<>();
        Path directoryPath = Path.of(dto.getFilePath());

        try (Stream<Path> paths = Files.walk(directoryPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(dto.getFileNameOrSuffix()))
                    .forEach(path -> {
                        log.info("===============================================");
                        log.info("Processando o PDF: {}", path.getFileName());
                        try {
                            list.add(service.document.processPdfFile(path, dto.getFileNameOrSuffix()));
                        } catch (MailReaderException e) {
                            log.error("Erro ao processar pdf - {} : {}", dto.getFilePathName(), e.getMessage());
                        }
                    });
        } catch (IOException e) {
            throw new MailReaderException(e.getMessage());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/process-pdf-with-python")
    public ResponseEntity<String> processPdfWithPython(@RequestBody PdfDTO dto) throws MailReaderException {
        return ResponseEntity.ok(pythonExtractor.extractText(Path.of(dto.getFilePathName()).toFile()));
    }
}
