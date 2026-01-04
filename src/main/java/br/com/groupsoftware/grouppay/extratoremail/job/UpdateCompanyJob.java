package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.UpdateCompany;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.CompanyUpdateStatus;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ClientGroupDTO;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Job responsável por processar e atualizar empresas no sistema.
 * <p>
 * Esse job busca todas as empresas pendentes de atualização na tabela {@link UpdateCompany}
 * e tenta atualizar cada uma delas utilizando o serviço {@link ServiceFacade}.
 * Se a atualização for bem-sucedida, o status da empresa é alterado para {@link CompanyUpdateStatus#UPDATED}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@RequiredArgsConstructor
@Slf4j
@Service
@DisallowConcurrentExecution
public class UpdateCompanyJob implements Job {

    private final RepositoryFacade repository;
    private final ServiceFacade service;
    private final ObjectMapper objectMapper;

    private static final int TAMANHO_LOTE = 100;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[COMPANY_UPDATE] Iniciando o job de atualização de empresa.");
        // Busca empresas não atualizadas

        Pageable pageable = PageRequest.of(0, TAMANHO_LOTE);
        List<UpdateCompany> companiesToProcess = repository.companyUpdate.findAllByUpdateStatus(CompanyUpdateStatus.NOT_UPDATED, pageable);

        List<UpdateCompany> companiesToUpdateStatus = new ArrayList<>();
        if (!companiesToProcess.isEmpty()) {
            log.info("[COMPANY_UPDATE] Atualizando {} empresas", companiesToProcess.size());
            companiesToProcess.forEach(updateCompany -> {
                boolean success = false;
                try {
                    service.groupPay.createUpdateCompany(objectMapper.readValue(updateCompany.getJson(), ClientGroupDTO.class));
                    success = true;
                } catch (JsonProcessingException e) {
                    log.error("Erro ao processar JSON para CompanyUpdate ID: {} - Erro: {}", updateCompany.getId(), e.getMessage(), e);
                } finally {
                    // Atualiza o status apenas se o processamento foi bem-sucedido
                    if (success) {
                        updateCompany.setUpdateStatus(CompanyUpdateStatus.UPDATED);
                        companiesToUpdateStatus.add(updateCompany);
                    // repository.companyUpdate.save(updateCompany);
                    }
                }
            });

            if (!companiesToUpdateStatus.isEmpty()) {
                log.info("[COMPANY_UPDATE] Salvando o status de {} empresas processadas.", companiesToUpdateStatus.size());
                repository.companyUpdate.saveAll(companiesToUpdateStatus);
            }
        } else {
            log.info("Nenhum documento encontrado para envio ao S3.");
        }
    }
}
