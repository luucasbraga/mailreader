package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.repository.DocumentRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para GroupPayServiceImpl
 * 
 * Testa o processamento de respostas do GroupPay, incluindo:
 * - Atualização de estágio baseado na resposta
 * - Salvamento de caminho S3
 * - Atualização de histórico de estágios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupPayServiceImpl - Testes Unitários")
class GroupPayServiceImplTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private GroupPayServiceImpl groupPayService;

    private Document document;
    private ClientGroup clientGroup;
    private Company company;
    private DocumentDTO documentDTO;

    @BeforeEach
    void setUp() {
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
                .clientGroup(clientGroup)
                .company(company)
                .build();

        documentDTO = DocumentDTO.builder()
                .documentId(1L)
                .amazonPath("s3://bucket/path/document.pdf")
                .build();

        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "document", documentRepository);
    }

    @Test
    @DisplayName("Deve atualizar estágio para SENT_TO_S3 quando há caminho S3")
    void deveAtualizarEstagioParaSentToS3() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // Act
        groupPayService.processExpense(documentDTO);

        // Assert
        verify(documentRepository).save(argThat(doc -> 
            doc.getStage() == DocumentStage.SENT_TO_S3 &&
            doc.getAmazonPath().equals("s3://bucket/path/document.pdf")
        ));
    }

    @Test
    @DisplayName("Deve atualizar estágio para DELETE_FROM_LOCAL quando não há caminho S3")
    void deveAtualizarEstagioParaDeleteFromLocal() {
        // Arrange
        DocumentDTO documentDTOSemS3 = DocumentDTO.builder()
                .documentId(1L)
                .amazonPath(null)
                .build();
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // Act
        groupPayService.processExpense(documentDTOSemS3);

        // Assert
        verify(documentRepository).save(argThat(doc -> 
            doc.getStage() == DocumentStage.DELETE_FROM_LOCAL
        ));
    }

    @Test
    @DisplayName("Deve lançar exceção quando documento não encontrado")
    void deveLancarExcecaoQuandoDocumentoNaoEncontrado() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            groupPayService.processExpense(documentDTO);
        });
    }

    @Test
    @DisplayName("Deve adicionar entrada no histórico de estágios")
    void deveAdicionarEntradaNoHistorico() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // Act
        groupPayService.processExpense(documentDTO);

        // Assert
        verify(documentRepository).save(argThat(doc -> 
            doc.getStagesHistory() != null && !doc.getStagesHistory().isEmpty()
        ));
    }
}

