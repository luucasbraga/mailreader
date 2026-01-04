package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNF;
import br.com.groupsoftware.grouppay.extratoremail.repository.DocumentRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.PdfService;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ExtractorExpenseJob
 * 
 * Testa a extração de dados estruturados dos documentos, incluindo:
 * - Busca de documentos com estágio TEXT_EXTRACTED
 * - Extração de Expense do documento
 * - Conversão para JSON
 * - Atualização de estágio para EXPENSE_EXTRACTED
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExtractorExpenseJob - Testes Unitários")
class ExtractorExpenseJobTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private ServiceFacade service;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private PdfService pdfService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExtractorExpenseJob extractorExpenseJob;

    private Document document;
    private ClientGroup clientGroup;
    private ExpenseNF expenseNF;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(extractorExpenseJob, "documentProcessingRetryDelay", 10);
        ReflectionTestUtils.setField(extractorExpenseJob, "objectMapper", objectMapper);

        clientGroup = ClientGroup.builder()
                .id(1L)
                .uuid("uuid-client-group")
                .build();

        document = Document.builder()
                .id(1L)
                .fileName("nota_fiscal_001.pdf")
                .messageId("msg-123")
                .stage(DocumentStage.TEXT_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .textExtracted("Texto extraído do PDF")
                .build();

        expenseNF = new ExpenseNF();
        expenseNF.setValorTotal(BigDecimal.valueOf(1000.00));
        expenseNF.setDataEmissao(LocalDate.now());
        expenseNF.setDataVencimento(LocalDate.now().plusDays(30));
        expenseNF.setCnpjCpfEmitente("12345678000190");
        expenseNF.setCompanyUUID(clientGroup.getUuid());

        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "document", documentRepository);
        ReflectionTestUtils.setField(service, "document", documentService);
        ReflectionTestUtils.setField(service, "pdf", pdfService);
    }

    @Test
    @DisplayName("Deve extrair Expense de documento com estágio TEXT_EXTRACTED")
    void deveExtrairExpenseDeDocumento() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(expenseNF)).thenReturn("{\"valorTotal\":1000.00}");

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(pdfService).getExpenseFromDocument(document);
        verify(objectMapper).writeValueAsString(expenseNF);
        verify(documentService).changeStage(document, DocumentStage.EXPENSE_EXTRACTED);
        assertNotNull(document.getExpenseJson());
    }

    @Test
    @DisplayName("Deve definir companyUUID no Expense antes de salvar")
    void deveDefinirCompanyUUIDNoExpense() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        assertEquals(clientGroup.getUuid(), expenseNF.getCompanyUUID());
    }

    @Test
    @DisplayName("Não deve processar quando não há documentos elegíveis")
    void naoDeveProcessarQuandoNaoHaDocumentos() throws Exception {
        // Arrange
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(pdfService, never()).getExpenseFromDocument(any());
        verify(documentService, never()).changeStage(any(), any());
    }

    @Test
    @DisplayName("Deve atualizar estágio para ERRO quando extração falhar")
    void deveAtualizarEstagioParaErroQuandoExtracaoFalhar() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenThrow(new RuntimeException("Erro na extração"));

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStage(document, DocumentStage.ERRO);
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Não deve atualizar estágio quando Expense é null")
    void naoDeveAtualizarEstagioQuandoExpenseENull() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenReturn(null);

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(documentService, never()).changeStage(any(), eq(DocumentStage.EXPENSE_EXTRACTED));
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING durante processamento")
    void deveAtualizarStatusParaProcessing() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.PROCESSING);
    }

    @Test
    @DisplayName("Deve restaurar status para NOT_PROCESSING após processamento")
    void deveRestaurarStatusAposProcessamento() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(pdfService.getExpenseFromDocument(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve tratar exceções sem interromper processamento de outros documentos")
    void deveTratarExcecoes() throws Exception {
        // Arrange
        Document document2 = Document.builder()
                .id(2L)
                .fileName("nota_fiscal_002.pdf")
                .stage(DocumentStage.TEXT_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .build();

        ExpenseNF expenseNF2 = new ExpenseNF();
        expenseNF2.setValorTotal(BigDecimal.valueOf(2000.00));
        expenseNF2.setCompanyUUID(clientGroup.getUuid());

        List<Document> documents = List.of(document, document2);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.TEXT_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        
        when(pdfService.getExpenseFromDocument(document)).thenThrow(new RuntimeException("Erro"));
        when(pdfService.getExpenseFromDocument(document2)).thenReturn(expenseNF2);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        extractorExpenseJob.execute(jobExecutionContext);

        // Assert - Deve processar o segundo documento mesmo com erro no primeiro
        verify(pdfService, times(2)).getExpenseFromDocument(any());
        verify(documentService, atLeastOnce()).changeStatus(any(), eq(Status.NOT_PROCESSING));
    }
}

