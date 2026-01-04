package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;

/**
 * Serviço responsável por identificar qual Company (condomínio)
 * um documento pertence com base nos dados extraídos do PDF.
 *
 * <p>Este serviço implementa múltiplas estratégias de matching para associar
 * documentos processados às Companies corretas dentro de um ClientGroup:</p>
 *
 * <ul>
 *   <li><strong>CNPJ do destinatário</strong> - Estratégia de maior confiabilidade</li>
 *   <li><strong>Nome fantasia do destinatário</strong> - Match por nome normalizado</li>
 *   <li><strong>Razão social do destinatário</strong> - Match por razão social normalizada</li>
 *   <li><strong>Matching fuzzy</strong> - Para casos com pequenas diferenças textuais</li>
 * </ul>
 *
 * <p>O matching é essencial no novo fluxo onde emails são centralizados por
 * ClientGroup ao invés de individualizados por Company.</p>
 *
 * @author Lucas Braga
 * @version 1.0
 * @since 2025
 * @see br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document
 * @see br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company
 * @see br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup
 */
public interface CompanyMatchingService {
    void matchDocumentToCompany(Document document);
}