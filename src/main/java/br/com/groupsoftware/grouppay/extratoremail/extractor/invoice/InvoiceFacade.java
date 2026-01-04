package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade para centralizar o acesso aos diferentes tipos de extractors de notas fiscais.
 * Esta classe facilita a injeção e a gestão de instâncias de extratores específicos,
 * permitindo que outros componentes do sistema acessem e utilizem os serviços de extração
 * de dados de notas fiscais de forma simplificada.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@RequiredArgsConstructor
@Component
public class InvoiceFacade {
    public final NfExtractor nf;
    public final Nf3Extractor nf3;
    public final NfsExtractor nfs;
    public final NfcExtractor nfc;
    public final CtExtractor ct;
}