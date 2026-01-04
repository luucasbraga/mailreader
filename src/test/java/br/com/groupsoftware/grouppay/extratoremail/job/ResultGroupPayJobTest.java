package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.repository.DocumentRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseSenderService;
import br.com.groupsoftware.grouppay.extratoremail.service.GroupPayService;
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
 * Testes unitários para ResultGroupPayJob
 * 
 * Testa o processamento de respostas do GroupPay Core, incluindo:
 * - Busca de documentos com estágio SENT_TO_GROUP_PAY
 * - Consulta de resposta do GroupPay
 * - Atualização de estágio baseado na resposta
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResultGroupPayJob - Testes Unitários")
class ResultGroupPayJobTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private ServiceFacade service;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private ExpenseSenderService expenseSenderService;

    @Mock
    private GroupPayService groupPayService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private ResultGroupPayJob resultGroupPayJob;

    private Document document;
    private ClientGroup clientGroup;
    private Company company;
    private DocumentDTO documentDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resultGroupPayJob, "documentProcessingRetryDelay", 10);

        clientGroup = ClientGroup.builder()
                .id(1L)
                .uuid("uuid-client-group")
                .build();

        company = Company.builder()
                .id(1L)
                .uuid("uuid-company")
                .clientGroup(clientGroup)
                .build();

        document = Document.builder()
                .id(1L)
                .fileName("nota_fiscal_001.pdf")
                .messageId("msg-123")
                .stage(DocumentStage.SENT_TO_GROUP_PAY)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .company(company)
                .build();

        documentDTO = DocumentDTO.builder()
                .documentId(1L)
                .amazonPath("s3://bucket/path/document.pdf")
                .build();

        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "document", documentRepository);
        ReflectionTestUtils.setField(service, "document", documentService);
        ReflectionTestUtils.setField(service, "expenseSender", expenseSenderService);
        ReflectionTestUtils.setField(service, "groupPay", groupPayService);
    }

    @Test
    @DisplayName("Deve processar resposta do GroupPay quando há documentos")
    void deveProcessarRespostaDoGroupPay() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseSenderService.getResultExpense(document)).thenReturn(documentDTO);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(expenseSenderService).getResultExpense(document);
        verify(groupPayService).processExpense(documentDTO);
        verify(documentService).changeStatus(document, Status.PROCESSING);
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Não deve processar quando não há documentos elegíveis")
    void naoDeveProcessarQuandoNaoHaDocumentos() {
        // Arrange
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(expenseSenderService, never()).getResultExpense(any());
        verify(groupPayService, never()).processExpense(any());
    }

    @Test
    @DisplayName("Não deve processar quando resposta é null")
    void naoDeveProcessarQuandoRespostaENull() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseSenderService.getResultExpense(document)).thenReturn(null);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(expenseSenderService).getResultExpense(document);
        verify(groupPayService, never()).processExpense(any());
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING durante processamento")
    void deveAtualizarStatusParaProcessing() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseSenderService.getResultExpense(document)).thenReturn(documentDTO);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.PROCESSING);
    }

    @Test
    @DisplayName("Deve restaurar status para NOT_PROCESSING após processamento")
    void deveRestaurarStatusAposProcessamento() {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseSenderService.getResultExpense(document)).thenReturn(documentDTO);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve tratar exceções sem interromper processamento de outros documentos")
    void deveTratarExcecoes() {
        // Arrange
        Document document2 = Document.builder()
                .id(2L)
                .fileName("nota_fiscal_002.pdf")
                .stage(DocumentStage.SENT_TO_GROUP_PAY)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .company(company)
                .build();

        DocumentDTO documentDTO2 = DocumentDTO.builder()
                .documentId(2L)
                .amazonPath("s3://bucket/path/document2.pdf")
                .build();

        List<Document> documents = List.of(document, document2);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        
        when(expenseSenderService.getResultExpense(document)).thenThrow(new RuntimeException("Erro"));
        when(expenseSenderService.getResultExpense(document2)).thenReturn(documentDTO2);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert - Deve processar o segundo documento mesmo com erro no primeiro
        verify(expenseSenderService, times(2)).getResultExpense(any());
        verify(groupPayService, times(1)).processExpense(documentDTO2);
        verify(documentService, atLeastOnce()).changeStatus(any(), eq(Status.NOT_PROCESSING));
    }

    @Test
    @DisplayName("Deve processar múltiplos documentos em paralelo")
    void deveProcessarMultiplosDocumentos() {
        // Arrange
        Document document2 = Document.builder()
                .id(2L)
                .fileName("nota_fiscal_002.pdf")
                .stage(DocumentStage.SENT_TO_GROUP_PAY)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .company(company)
                .build();

        DocumentDTO documentDTO2 = DocumentDTO.builder()
                .documentId(2L)
                .amazonPath("s3://bucket/path/document2.pdf")
                .build();

        List<Document> documents = List.of(document, document2);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.SENT_TO_GROUP_PAY), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseSenderService.getResultExpense(document)).thenReturn(documentDTO);
        when(expenseSenderService.getResultExpense(document2)).thenReturn(documentDTO2);

        // Act
        resultGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(expenseSenderService, times(2)).getResultExpense(any());
        verify(groupPayService, times(2)).processExpense(any(DocumentDTO.class));
    }
}

