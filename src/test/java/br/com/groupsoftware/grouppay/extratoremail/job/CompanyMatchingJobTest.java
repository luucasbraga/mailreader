package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.repository.DocumentRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.CompanyMatchingService;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CompanyMatchingJob
 * 
 * Testa o matching de documentos com Companies, incluindo:
 * - Busca de documentos com estágio EXPENSE_EXTRACTED
 * - Execução do matching service
 * - Atualização de estágio para COMPANY_MATCHED ou COMPANY_NOT_FOUND
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyMatchingJob - Testes Unitários")
class CompanyMatchingJobTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private CompanyMatchingService companyMatchingService;

    @Mock
    private ServiceFacade service;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private CompanyMatchingJob companyMatchingJob;

    private Document document;
    private ClientGroup clientGroup;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(companyMatchingJob, "documentProcessingRetryDelay", 10);

        clientGroup = ClientGroup.builder()
                .id(1L)
                .uuid("uuid-client-group")
                .build();

        document = Document.builder()
                .id(1L)
                .fileName("nota_fiscal_001.pdf")
                .messageId("msg-123")
                .stage(DocumentStage.EXPENSE_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .expenseJson("{\"cnpj\":\"12345678000190\"}")
                .build();

        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "document", documentRepository);
        ReflectionTestUtils.setField(service, "document", documentService);
    }

    @Test
    @DisplayName("Deve processar documentos com estágio EXPENSE_EXTRACTED")
    void deveProcessarDocumentosComEstagioExpenseExtracted() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert
        verify(companyMatchingService, times(1)).matchDocumentToCompany(document);
        verify(documentService).changeStatus(document, Status.PROCESSING);
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Não deve processar quando não há documentos elegíveis")
    void naoDeveProcessarQuandoNaoHaDocumentos() {
        // Arrange
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert
        verify(companyMatchingService, never()).matchDocumentToCompany(any());
        verify(documentService, never()).changeStatus(any(), any());
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING durante matching")
    void deveAtualizarStatusParaProcessing() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.PROCESSING);
    }

    @Test
    @DisplayName("Deve restaurar status para NOT_PROCESSING após matching")
    void deveRestaurarStatusAposMatching() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve tratar exceções durante matching sem interromper outros")
    void deveTratarExcecoesDuranteMatching() {
        // Arrange
        Document document2 = Document.builder()
                .id(2L)
                .fileName("nota_fiscal_002.pdf")
                .stage(DocumentStage.EXPENSE_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .build();

        List<Document> documents = List.of(document, document2);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);
        
        doThrow(new RuntimeException("Erro no matching")).when(companyMatchingService)
                .matchDocumentToCompany(document);

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert - Deve processar o segundo documento mesmo com erro no primeiro
        verify(companyMatchingService, times(2)).matchDocumentToCompany(any());
        verify(documentService, atLeastOnce()).changeStatus(any(), eq(Status.NOT_PROCESSING));
    }

    @Test
    @DisplayName("Deve processar múltiplos documentos em paralelo")
    void deveProcessarMultiplosDocumentos() {
        // Arrange
        Document document2 = Document.builder()
                .id(2L)
                .fileName("nota_fiscal_002.pdf")
                .stage(DocumentStage.EXPENSE_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .build();

        Document document3 = Document.builder()
                .id(3L)
                .fileName("nota_fiscal_003.pdf")
                .stage(DocumentStage.EXPENSE_EXTRACTED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .build();

        List<Document> documents = List.of(document, document2, document3);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.EXPENSE_EXTRACTED), any(LocalDateTime.class)))
                .thenReturn(documents);

        // Act
        companyMatchingJob.execute(jobExecutionContext);

        // Assert
        verify(companyMatchingService, times(3)).matchDocumentToCompany(any(Document.class));
        verify(documentService, times(3)).changeStatus(any(), eq(Status.PROCESSING));
        verify(documentService, times(3)).changeStatus(any(), eq(Status.NOT_PROCESSING));
    }
}

