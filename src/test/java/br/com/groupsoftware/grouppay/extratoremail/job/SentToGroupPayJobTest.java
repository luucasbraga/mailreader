package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNF;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ExpenseDTO;
import br.com.groupsoftware.grouppay.extratoremail.repository.DocumentRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseMapperService;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseSenderService;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Testes unitários para SentToGroupPayJob
 * 
 * Testa o envio de despesas para o GroupPay Core, incluindo:
 * - Busca de documentos com estágio COMPANY_MATCHED
 * - Conversão de Expense para ExpenseDTO
 * - Envio via REST API
 * - Atualização de estágio para SENT_TO_GROUP_PAY
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SentToGroupPayJob - Testes Unitários")
class SentToGroupPayJobTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private ServiceFacade service;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private ExpenseMapperService expenseMapperService;

    @Mock
    private ExpenseSenderService expenseSenderService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SentToGroupPayJob sentToGroupPayJob;

    private Document document;
    private ClientGroup clientGroup;
    private Company company;
    private ExpenseNF expenseNF;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(sentToGroupPayJob, "documentProcessingRetryDelay", 10);
        ReflectionTestUtils.setField(sentToGroupPayJob, "objectMapper", objectMapper);

        // Setup entities
        clientGroup = ClientGroup.builder()
                .id(1L)
                .uuid("uuid-client-group")
                .codigoSuporte("123456")
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
                .stage(DocumentStage.COMPANY_MATCHED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .company(company)
                .expenseType(ExpenseType.NFE)
                .expenseJson("{\"valorTotal\":1000.00}")
                .build();

        expenseNF = new ExpenseNF();
        expenseNF.setValorTotal(BigDecimal.valueOf(1000.00));
        expenseNF.setDataEmissao(LocalDate.now());
        expenseNF.setDataVencimento(LocalDate.now().plusDays(30));
        expenseNF.setCnpjCpfEmitente("12345678000190");
        expenseNF.setCompanyUUID(clientGroup.getUuid());

        // Setup mocks
        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "document", documentRepository);
        ReflectionTestUtils.setField(service, "document", documentService);
        ReflectionTestUtils.setField(service, "expenseMapper", expenseMapperService);
        ReflectionTestUtils.setField(service, "expenseSender", expenseSenderService);
    }

    @Test
    @DisplayName("Deve enviar documento com estágio COMPANY_MATCHED para GroupPay")
    void deveEnviarDocumentoParaGroupPay() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseMapperService.getExpense(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"valorTotal\":1000.00}");

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

        // Assert
        ArgumentCaptor<ExpenseDTO> expenseDTOCaptor = ArgumentCaptor.forClass(ExpenseDTO.class);
        verify(expenseSenderService).sendExpense(eq(document), expenseDTOCaptor.capture());
        
        ExpenseDTO capturedDTO = expenseDTOCaptor.getValue();
        assertEquals(document.getId(), capturedDTO.getDocumentId());
        assertEquals(ExpenseType.NFE, capturedDTO.getType());
        assertEquals("123456", capturedDTO.getCodSupport());
        assertTrue(capturedDTO.getFileName().contains("nota_fiscal_001"));
        
        verify(documentService).changeStage(document, DocumentStage.SENT_TO_GROUP_PAY);
    }

    @Test
    @DisplayName("Não deve processar quando não há documentos elegíveis")
    void naoDeveProcessarQuandoNaoHaDocumentos() throws Exception {
        // Arrange
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(expenseSenderService, never()).sendExpense(any(), any());
        verify(documentService, never()).changeStage(any(), any());
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING durante processamento")
    void deveAtualizarStatusParaProcessing() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseMapperService.getExpense(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

        // Assert
        verify(documentService).changeStatus(document, Status.PROCESSING);
        verify(documentService).changeStatus(document, Status.NOT_PROCESSING);
    }

    @Test
    @DisplayName("Deve restaurar status para NOT_PROCESSING após processamento")
    void deveRestaurarStatusAposProcessamento() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseMapperService.getExpense(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

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
                .stage(DocumentStage.COMPANY_MATCHED)
                .status(Status.NOT_PROCESSING)
                .clientGroup(clientGroup)
                .company(company)
                .expenseType(ExpenseType.NFE)
                .build();

        List<Document> documents = List.of(document, document2);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(documents);
        
        when(expenseMapperService.getExpense(document)).thenThrow(new RuntimeException("Erro"));
        when(expenseMapperService.getExpense(document2)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

        // Assert - Deve processar o segundo documento mesmo com erro no primeiro
        verify(expenseSenderService, times(1)).sendExpense(eq(document2), any());
        verify(documentService, atLeastOnce()).changeStatus(any(), eq(Status.NOT_PROCESSING));
    }

    @Test
    @DisplayName("Deve adicionar timestamp ao nome do arquivo")
    void deveAdicionarTimestampAoNomeArquivo() throws Exception {
        // Arrange
        List<Document> documents = List.of(document);
        when(documentRepository.findDocumentsEligibleForProcessing(
                eq(DocumentStage.COMPANY_MATCHED), any(LocalDateTime.class)))
                .thenReturn(documents);
        when(expenseMapperService.getExpense(document)).thenReturn(expenseNF);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        sentToGroupPayJob.execute(jobExecutionContext);

        // Assert
        ArgumentCaptor<ExpenseDTO> expenseDTOCaptor = ArgumentCaptor.forClass(ExpenseDTO.class);
        verify(expenseSenderService).sendExpense(any(), expenseDTOCaptor.capture());
        
        ExpenseDTO capturedDTO = expenseDTOCaptor.getValue();
        assertTrue(capturedDTO.getFileName().contains("nota_fiscal_001"));
        assertTrue(capturedDTO.getFileName().matches(".*_\\d{14}\\.pdf")); // Formato: nome_yyyyMMddHHmmss.pdf
    }
}

