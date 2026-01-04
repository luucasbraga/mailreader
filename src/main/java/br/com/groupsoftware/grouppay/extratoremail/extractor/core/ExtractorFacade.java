package br.com.groupsoftware.grouppay.extratoremail.extractor.core;

import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.InvoiceFacade;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.SlipFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade para centralizar o acesso aos extractors.
 * <p>Esta classe atua como uma fachada para agrupar e facilitar o acesso aos diferentes serviços de extração de dados,
 * incluindo extração com OpenAI, PDF parsing, extração com script Python e serviços de processamento de notas fiscais.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@RequiredArgsConstructor
@Component
public class ExtractorFacade {
    public final OpenAiExtractor openAi;
    public final GeminiExtractor gemini;
    public final PdfExtractor pdf;
    public final PythonExtractor python;
    public final InvoiceFacade invoice;
    public final SlipFacade slip;
    public final OcrExtractor ocr;
}
