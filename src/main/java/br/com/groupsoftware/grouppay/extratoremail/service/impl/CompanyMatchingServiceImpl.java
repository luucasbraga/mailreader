package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.CompanyMatchingService;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável por identificar qual Company (condomínio)
 * um documento pertence com base nos dados extraídos.
 *
 * @author Lucas Braga
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyMatchingServiceImpl implements CompanyMatchingService {

    private final RepositoryFacade repository;
    private final ExpenseMapperService expenseMapperService;
    private final DocumentService documentService;

    @Override
    public void matchDocumentToCompany(Document document) {
        try {
            Expense expense = expenseMapperService.getExpense(document);
            if (expense == null) {
                log.warn("Não foi possível obter despesa do documento: {}", document.getFileName());
                return;
            }

            List<Company> companies = repository.company.findByClientGroup(document.getClientGroup());
            Optional<Company> companyOp = findMatchingCompany(expense, companies);

            if (companyOp.isPresent()) {
                document.setCompany(companyOp.get());
                document.setExpenseJson(document.getExpenseJson().replace(document.getClientGroup().getUuid(), companyOp.get().getUuid()));
                documentService.changeStage(document, DocumentStage.COMPANY_MATCHED);
                log.info("Documento {} associado à company: {} (CNPJ: {})",
                        document.getFileName(), companyOp.get().getUuid(), companyOp.get().getCnpj());
            }
            else {
                documentService.changeStage(document, DocumentStage.COMPANY_NOT_FOUND);
                document.setCompany(null);
                log.warn("Não foi possível associar documento {} a nenhuma company. CNPJ destinatário: {} | Emitente: {}",
                        document.getFileName(), expense.getCnpjCpfDestinatario(), expense.getEmitente());
            }

        } catch (Exception e) {
            log.error("Erro ao fazer matching do documento: {}", document.getFileName(), e);
        }
    }

    private Optional<Company> findMatchingCompany(Expense expense, List<Company> companies) {
        String cnpjDestino = extractCnpjDestino(expense);
        if (cnpjDestino != null && !cnpjDestino.trim().isEmpty()) {
            String cnpjDestinoNormalizado = normalizeCnpj(cnpjDestino);
            
            Optional<Company> matchExato = companies.stream()
                    .filter(company -> {
                        String cnpjCompany = normalizeCnpj(company.getCnpj());
                        return cnpjCompany != null && cnpjCompany.equals(cnpjDestinoNormalizado);
                    })
                    .findFirst();

            if (matchExato.isPresent()) {
                log.debug("Match exato encontrado por CNPJ completo: {}", cnpjDestinoNormalizado);
                return matchExato;
            }

            // Se não encontrou exato e o CNPJ tem menos de 14 dígitos, tenta match parcial (primeiros 8 dígitos - raiz do CNPJ)
            if (cnpjDestinoNormalizado.length() < 14 && cnpjDestinoNormalizado.length() >= 8) {
                String raizCnpjDestino = cnpjDestinoNormalizado.substring(0, 8);
                Optional<Company> matchParcial = companies.stream()
                        .filter(company -> {
                            String cnpjCompany = normalizeCnpj(company.getCnpj());
                            if (cnpjCompany != null && cnpjCompany.length() >= 8) {
                                String raizCnpjCompany = cnpjCompany.substring(0, 8);
                                return raizCnpjCompany.equals(raizCnpjDestino);
                            }
                            return false;
                        })
                        .findFirst();

                if (matchParcial.isPresent()) {
                    log.debug("Match parcial encontrado por raiz do CNPJ (primeiros 8 dígitos): {} -> {}", 
                            raizCnpjDestino, matchParcial.get().getCnpj());
                    return matchParcial;
                }
            }
        }

        // Se não encontrou por CNPJ, tenta por nome fantasia/razão social
        String nomeDestino = extractNomeDestino(expense);
        if (nomeDestino != null && !nomeDestino.trim().isEmpty()) {
            Optional<Company> matchNome = companies.stream()
                    .filter(company -> isNomeMatch(nomeDestino, company))
                    .findFirst();

            if (matchNome.isPresent()) {
                log.debug("Match encontrado por nome: {} -> {}", nomeDestino, matchNome.get().getFantasyName());
                return matchNome;
            }
        }

        return Optional.empty();
    }

    private String extractCnpjDestino(Expense expense) {
        if (expense == null) {
            return null;
        }
        
        try {
            // Para a maioria dos documentos, o CNPJ do destinatário está no campo cnpjCpfDestinatario
            String cnpjDestinatario = expense.getCnpjCpfDestinatario();
            if (cnpjDestinatario != null && !cnpjDestinatario.trim().isEmpty() && !"null".equalsIgnoreCase(cnpjDestinatario.trim())) {
                return cnpjDestinatario;
            }
            // Adicionar lógica para outros tipos de documento
        } catch (Exception e) {
            log.debug("Erro ao extrair CNPJ do destinatário: {}", e.getMessage());
        }
        return null;
    }

    private String extractNomeDestino(Expense expense) {
        if (expense == null) {
            return null;
        }
        
        try {
            if (expense.getExpenseType() != null) {
                String expenseType = expense.getExpenseType().name();
                boolean isBoletoOuFatura = "BOLETO".equals(expenseType) || "FATURA".equals(expenseType);
                boolean isNotaFiscal = "NFE".equals(expenseType) || "NFSE".equals(expenseType);
                
                if (isBoletoOuFatura) {
                    // 1. Campo cedente (mais comum em boletos de concessionárias)
                    if (expense instanceof br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseBoleto) {
                        br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseBoleto boleto = 
                            (br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseBoleto) expense;
                        String cedente = boleto.getCedente();
                        if (cedente != null && !cedente.trim().isEmpty() && !"null".equalsIgnoreCase(cedente.trim())) {
                            String cedenteLimpo = cedente.replaceAll("\\D", "");
                            if (cedenteLimpo.length() < 11 || cedenteLimpo.length() > 14) {
                                return cedente;
                            }
                        }
                    }
                    
                    // 2. Campo cnpjCpfDestinatario (pode conter nome quando não há CNPJ)
                    String cnpjDestinatario = expense.getCnpjCpfDestinatario();
                    if (cnpjDestinatario != null && !cnpjDestinatario.trim().isEmpty() && !"null".equalsIgnoreCase(cnpjDestinatario.trim())) {
                        String cnpjLimpo = cnpjDestinatario.replaceAll("\\D", "");
                        // Se não parece ser um CNPJ/CPF (menos de 11 ou mais de 14 dígitos), pode ser nome
                        if (cnpjLimpo.length() < 11 || cnpjLimpo.length() > 14) {
                            return cnpjDestinatario;
                        }
                    }
                    
                    // 3. Campo emitente (fallback, menos confiável para concessionárias)
                    String emitente = expense.getEmitente();
                    if (emitente != null && !emitente.trim().isEmpty() && !"null".equalsIgnoreCase(emitente.trim())) {
                        String emitenteLimpo = emitente.replaceAll("\\D", "");
                        if (emitenteLimpo.length() < 11 || emitenteLimpo.length() > 14) {
                            return emitente;
                        }
                    }
                } else if (isNotaFiscal) {
                    // Para notas fiscais, o cnpjCpfDestinatario pode conter nome quando não há CNPJ
                    // (caso comum quando extractors regex não extraem corretamente)
                    String cnpjDestinatario = expense.getCnpjCpfDestinatario();
                    if (cnpjDestinatario != null && !cnpjDestinatario.trim().isEmpty() && !"null".equalsIgnoreCase(cnpjDestinatario.trim())) {
                        String cnpjLimpo = cnpjDestinatario.replaceAll("\\D", "");
                        // Se não parece ser um CNPJ/CPF (menos de 11 ou mais de 14 dígitos), pode ser nome
                        if (cnpjLimpo.length() < 11 || cnpjLimpo.length() > 14) {
                            return cnpjDestinatario;
                        }
                    }
                    // Para notas fiscais, não usamos emitente como fallback pois é o fornecedor, não o condomínio
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair nome do destinatário: {}", e.getMessage());
        }
        return null;
    }

    private String normalizeCnpj(String cnpj) {
        if (cnpj == null) return null;
        // Remove todos os caracteres não numéricos (pontos, barras, hífens, espaços, etc)
        return cnpj.replaceAll("\\D", "");
    }

    private boolean isNomeMatch(String nomeDocumento, Company company) {
        if (nomeDocumento == null) return false;

        // Normaliza nomes para comparação
        String nomeDoc = nomeDocumento.toLowerCase().trim();

        // Verifica nome fantasia
        if (company.getFantasyName() != null) {
            String nomeFantasia = company.getFantasyName().toLowerCase().trim();
            if (nomeDoc.contains(nomeFantasia) || nomeFantasia.contains(nomeDoc)) {
                return true;
            }
        }

        // Verifica razão social
        if (company.getLegalName() != null) {
            String razaoSocial = company.getLegalName().toLowerCase().trim();
            if (nomeDoc.contains(razaoSocial) || razaoSocial.contains(nomeDoc)) {
                return true;
            }
        }

        return false;
    }
}