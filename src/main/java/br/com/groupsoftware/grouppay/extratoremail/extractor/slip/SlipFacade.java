package br.com.groupsoftware.grouppay.extratoremail.extractor.slip;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade para centralizar o acesso aos diferentes tipos de extractors de boletos.
 * <p>
 * Esta classe facilita a injeção e a gestão de instâncias de extratores específicos,
 * permitindo que outros componentes do sistema acessem e utilizem os serviços de extração
 * de dados de boletos de forma simplificada.
 * </p>
 * <p>
 * Cada campo representa um extrator especializado para um tipo de boleto, possibilitando
 * a extração de informações necessárias para o processamento e pagamento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@RequiredArgsConstructor
@Component
public class SlipFacade {
    public final BillExtractor bill;
    public final BankSlipExtractor bank;
}
