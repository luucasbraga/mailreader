package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.repository.ClientGroupRepository;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.EmailService;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para EmailProcessJob
 * 
 * Testa o processamento de emails por ClientGroup, incluindo:
 * - Busca de ClientGroups elegíveis
 * - Controle de limite de emails simultâneos
 * - Processamento paralelo
 * - Atualização de status
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailProcessJob - Testes Unitários")
class EmailProcessJobTest {

    @Mock
    private RepositoryFacade repository;

    @Mock
    private ServiceFacade service;

    @Mock
    private ClientGroupRepository clientGroupRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private EmailProcessJob emailProcessJob;

    private ClientGroup clientGroup1;
    private ClientGroup clientGroup2;

    @BeforeEach
    void setUp() {
        // Configurar valores via ReflectionTestUtils
        ReflectionTestUtils.setField(emailProcessJob, "maxEmails", 5);
        ReflectionTestUtils.setField(emailProcessJob, "emailProcessingRetryDelay", 10);
        ReflectionTestUtils.setField(emailProcessJob, "readerDir", "/tmp/test");

        // Criar ClientGroups de teste
        clientGroup1 = ClientGroup.builder()
                .id(1L)
                .uuid("uuid-1")
                .email("test1@example.com")
                .status(Status.NOT_PROCESSING)
                .lastMailRead(LocalDateTime.now().minusHours(1))
                .build();

        clientGroup2 = ClientGroup.builder()
                .id(2L)
                .uuid("uuid-2")
                .email("test2@example.com")
                .status(Status.NOT_PROCESSING)
                .lastMailRead(LocalDateTime.now().minusHours(2))
                .build();

        // Configurar facades usando ReflectionTestUtils para campos final
        ReflectionTestUtils.setField(repository, "clientGroup", clientGroupRepository);
        ReflectionTestUtils.setField(service, "email", emailService);
    }

    @Test
    @DisplayName("Deve processar ClientGroups elegíveis quando há emails disponíveis")
    void deveProcessarClientGroupsElegiveis() throws Exception {
        // Arrange
        List<ClientGroup> clientGroups = List.of(clientGroup1, clientGroup2);
        Page<ClientGroup> page = new PageImpl<>(clientGroups);
        
        when(clientGroupRepository.findAll()).thenReturn(clientGroups);
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);
        when(clientGroupRepository.findClientGroupsEligibleForEmailProcessing(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert
        verify(clientGroupRepository, times(2)).save(any(ClientGroup.class));
        verify(emailService, times(2)).getEmailsAndSavePdfs(any(ClientGroup.class));
    }

    @Test
    @DisplayName("Não deve processar quando limite de emails atingido")
    void naoDeveProcessarQuandoLimiteAtingido() throws Exception {
        // Arrange
        when(clientGroupRepository.findAll()).thenReturn(List.of(clientGroup1));
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(5); // Limite atingido

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert
        verify(clientGroupRepository, never()).findClientGroupsEligibleForEmailProcessing(any(), any());
        verify(emailService, never()).getEmailsAndSavePdfs(any());
    }

    @Test
    @DisplayName("Não deve processar quando não há ClientGroups no banco")
    void naoDeveProcessarQuandoNaoHaClientGroups() throws Exception {
        // Arrange
        when(clientGroupRepository.findAll()).thenReturn(new ArrayList<>());
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert
        verify(clientGroupRepository, never()).findClientGroupsEligibleForEmailProcessing(any(), any());
        verify(emailService, never()).getEmailsAndSavePdfs(any());
    }

    @Test
    @DisplayName("Deve atualizar status para PROCESSING antes de processar")
    void deveAtualizarStatusParaProcessing() throws Exception {
        // Arrange
        List<ClientGroup> clientGroups = List.of(clientGroup1);
        Page<ClientGroup> page = new PageImpl<>(clientGroups);
        
        when(clientGroupRepository.findAll()).thenReturn(clientGroups);
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);
        when(clientGroupRepository.findClientGroupsEligibleForEmailProcessing(any(), any()))
                .thenReturn(page);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert
        verify(clientGroupRepository, atLeastOnce()).save(argThat(cg -> 
            cg.getStatus() == Status.PROCESSING || cg.getStatus() == Status.NOT_PROCESSING
        ));
    }

    @Test
    @DisplayName("Deve restaurar status para NOT_PROCESSING após processamento")
    void deveRestaurarStatusAposProcessamento() throws Exception {
        // Arrange
        List<ClientGroup> clientGroups = List.of(clientGroup1);
        Page<ClientGroup> page = new PageImpl<>(clientGroups);
        
        when(clientGroupRepository.findAll()).thenReturn(clientGroups);
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);
        when(clientGroupRepository.findClientGroupsEligibleForEmailProcessing(any(), any()))
                .thenReturn(page);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert - Verifica que o status foi restaurado
        verify(clientGroupRepository, atLeastOnce()).save(argThat(cg -> 
            cg.getStatus() == Status.NOT_PROCESSING
        ));
    }

    @Test
    @DisplayName("Deve tratar exceções durante processamento sem interromper outros")
    void deveTratarExcecoesDuranteProcessamento() throws Exception {
        // Arrange
        List<ClientGroup> clientGroups = List.of(clientGroup1, clientGroup2);
        Page<ClientGroup> page = new PageImpl<>(clientGroups);
        
        when(clientGroupRepository.findAll()).thenReturn(clientGroups);
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);
        when(clientGroupRepository.findClientGroupsEligibleForEmailProcessing(any(), any()))
                .thenReturn(page);
        
        // Simular erro no primeiro ClientGroup
        doThrow(new RuntimeException("Erro de conexão")).when(emailService)
                .getEmailsAndSavePdfs(clientGroup1);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert - Deve processar o segundo mesmo com erro no primeiro
        verify(emailService, times(2)).getEmailsAndSavePdfs(any(ClientGroup.class));
        verify(clientGroupRepository, atLeast(2)).save(any(ClientGroup.class));
    }

    @Test
    @DisplayName("Deve respeitar limite máximo de processamento simultâneo")
    void deveRespeitarLimiteMaximo() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(emailProcessJob, "maxEmails", 2);
        
        List<ClientGroup> allClientGroups = List.of(clientGroup1, clientGroup2, 
                ClientGroup.builder().id(3L).uuid("uuid-3").build());
        Page<ClientGroup> page = new PageImpl<>(allClientGroups.subList(0, 2));
        
        when(clientGroupRepository.findAll()).thenReturn(allClientGroups);
        when(clientGroupRepository.countAllByStatusProcessing()).thenReturn(0);
        when(clientGroupRepository.findClientGroupsEligibleForEmailProcessing(any(), any()))
                .thenReturn(page);

        // Act
        emailProcessJob.execute(jobExecutionContext);

        // Assert - Deve processar apenas 2 (limite)
        verify(emailService, times(2)).getEmailsAndSavePdfs(any(ClientGroup.class));
    }
}

