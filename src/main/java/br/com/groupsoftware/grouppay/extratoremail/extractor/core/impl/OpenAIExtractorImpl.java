package br.com.groupsoftware.grouppay.extratoremail.extractor.core.impl;

import br.com.groupsoftware.grouppay.extratoremail.config.OpenAIConfig;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.AIMessageModel;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.OpenAIDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.OpenAIMessageDTO;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.OpenAiExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.util.DocumentUtils;
import br.com.groupsoftware.grouppay.extratoremail.util.brazil.CpfCnpjUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementação do serviço de extração de informações financeiras utilizando a API da OpenAI.
 * <p>
 * Processa texto extraído de documentos, como PDF, e converte em objetos de modelo financeiro,
 * retornando uma despesa mapeada conforme o tipo de documento (NF, Boleto, Fatura, etc.).
 * Utiliza a API da OpenAI para realizar a conversão de texto em JSON estruturado com base em exemplos predefinidos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIExtractorImpl implements OpenAiExtractor {

    private final OpenAIConfig config;
    private final ObjectMapper objectMapper;
    private final RepositoryFacade repository;

    @Override
    public Expense getExpense(Document document, ExpenseType type) {
        try {
            AiPlanType planType = DocumentUtils.getAiPlanType(document);
            AIMessageModel aiMessageModel = repository.aiMessageModel
                    .findByExpenseTypeAndPlanType(type, planType != null ? planType : AiPlanType.BASIC);

            OpenAIDTO request = buildRequest(document.getTextExtracted(), aiMessageModel);
            ResponseEntity<String> responseEntity = sendRequest(request);

            logTokensUsage(responseEntity.getBody());
            return mapJsonToExpense(processAIResponse(responseEntity.getBody()), document, type);

        } catch (HttpClientErrorException e) {
            log.error("Erro ao se comunicar com a API da OpenAI: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar a despesa: {}", e.getMessage(), e);
        }
        return null;
    }

    private ResponseEntity<String> sendRequest(OpenAIDTO request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), createHeaders());
            return restTemplate.postForEntity(config.getUrl(), entity, String.class);
        } catch (Exception e) {
            log.error("Erro ao enviar a requisição para a API da OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar a requisição para a API da OpenAI.");
        }
    }

    private void logTokensUsage(String responseBody) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");

            log.info("Tokens usados - Prompt: {}, Resposta: {}, Total: {}",
                    usage.get("prompt_tokens"), usage.get("completion_tokens"), usage.get("total_tokens"));
        } catch (Exception e) {
            log.error("Erro ao registrar o uso de tokens: {}", e.getMessage(), e);
        }
    }

    private Expense mapJsonToExpense(String json, Document document, ExpenseType type) {
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.findAndRegisterModules();

            Class<?> expenseClass = config.getExpenseTypeMap().get(type);
            if (expenseClass == null) {
                log.error("Tipo de despesa não reconhecido: {}", type);
                return null;
            }

            return (Expense) objectMapper.readValue(json, expenseClass);

        } catch (Exception e) {
            log.error("Erro ao extrair json: {}", e.getMessage(), e);
            document.setStage(DocumentStage.DELETE_FROM_LOCAL);
            document.addStageHistoryEntry();
            repository.document.save(document);
        }
        return null;
    }


    private OpenAIDTO buildRequest(String extractedText, AIMessageModel aiMessageModel) {
        OpenAIMessageDTO message = new OpenAIMessageDTO("user", getUserMessage(extractedText, aiMessageModel));
        return new OpenAIDTO(config.getModel(), Collections.singletonList(message));
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getKey());
        return headers;
    }

    private String getUserMessage(String extractedText, AIMessageModel aiMessageModel) {
        return String.format(
                """
                        Extraia informações do texto fornecido e retorne como JSON seguindo o modelo abaixo. Utilize o exemplo apenas como molde, não copie os valores – extraia os dados reais conforme os campos encontrados.
                        
                        ### Regras Gerais:
                        1. **Campos Obrigatórios:**
                           - `dataEmissao`: Data de emissão (formato `dd/MM/yyyy` ou `yyyy-MM-dd`).
                           - `dataVencimento`: Data de vencimento.
                           - `valorTotal`: Valor total do documento.
                           - `emitente`: Nome ou razão social do emitente.
                           - `cnpjCpfEmitente`: CNPJ/CPF do emitente. **Atenção:** Valide e normalize removendo formatações (pontos, barras, hífens). Este valor deve corresponder ao emitente da nota e não à transportadora, a não ser que a transportadora seja a emissora.
                           - `cnpjCpfDestinatario`: CNPJ/CPF do destinatário (quando presente).
                        
                        2. **Dados Importantes Adicionais:**
                           - **Número da Nota (`numero`) e Série (`serie`):** Estes campos são essenciais e devem ser extraídos com precisão. Procure por indicadores como "Nº.", "Número da Nota", "Nota" e "Série" ou "SÉRIE" para identificar corretamente esses valores.
                        
                        3. **Adaptação de Termos:**
                           - Identifique e normalize CNPJs e CPFs em qualquer formato.
                           - Se o campo "Emitente" for identificado, relacione o CNPJ/CPF mais próximo como `cnpjCpfEmitente`.
                           - Associe o campo "Destinatário" ao nome e CNPJ/CPF mais próximos, se presentes.
                        
                        4. **Itens e Detalhamento:**
                           - Se o documento contiver itens, extraia-os em um array no campo `itens` com os atributos:
                             - `descricao`
                             - `quantidade`
                             - `valorUnitario`
                             - `valorTotalItem`
                           - Para documentos do tipo NF (ExpenseNF, ExpenseNF3, ExpenseNFC), cada item pode incluir uma lista de `impostos` com os campos:
                             - `tipoImposto` (ex.: ICMS, IPI, PIS)
                             - `valor`
                        
                        5. **Datas e Períodos:**
                           - Converta as datas para os formatos especificados (`dd/MM/yyyy` ou `yyyy-MM-dd`).
                           - Quando aplicável, extraia os campos `periodoInicio` e `periodoFim`.
                        
                        6. **Campos Específicos por Tipo de Documento:**
                           - **ExpenseBoleto:** Extraia também os campos `bancoEmissor`, `codigoBarras`, `linhaDigitavel`, `cedente`, `nossoNumero`, `juros`, `multa` e `descontos`.
                           - **ExpenseCT:** Extraia também os campos `remetente`, `veiculo`, `pesoCarga`, `tipoCarga` e `motorista`.
                           - **ExpenseFatura:** Extraia também o campo `valorPago` e a lista de itens conforme definido.
                           - **ExpenseNF:** Extraia também os campos `chaveAcesso`, `numero`, `serie`, `valorFrete`, `valorSeguro` e `descontos`, além dos itens com seus impostos.
                           - **ExpenseNF3:** Além dos campos de ExpenseNF, extraia `numeroMedidor`, `periodoInicio` e `periodoFim`.
                           - **ExpenseNFS:** Extraia também os campos `numero`, `serie`, `codigoVerificacao`, `descricaoServico`, `aliquotaISS`, `valorISS`, `descontos` e `valorLiquido`.
                        
                        7. **Instruções Adicionais:**
                           - Utilize o modelo JSON abaixo apenas como referência para a estrutura final.
                           - Se algum campo obrigatório não for identificado, retorne-o como `null` ou omita-o.
                           - A resposta deve ser **exclusivamente** o JSON final, sem comentários ou explicações adicionais.
                        
                        %s
                        
                        ### Texto do PDF:
                        %s
                        
                        ### Exemplo JSON (Molde):
                        %s
                        """,
                aiMessageModel.getRules(), extractedText, aiMessageModel.getJsonModel()
        );
    }

    private String processAIResponse(String aiResponse) {
        try {
            return getJson(aiResponse, objectMapper);
        } catch (Exception e) {
            log.error("Erro ao processar a resposta da IA: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar a resposta da IA: " + e.getMessage());
        }
    }

    public static String getJson(String aiResponse, ObjectMapper objectMapper) throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> extractedData = objectMapper.readValue(aiResponse, Map.class);
        List<Map<String, Object>> choicesList = (List<Map<String, Object>>) extractedData.get("choices");
        Map<String, Object> firstChoice = choicesList.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        String content = (String) message.get("content");
        content = content.replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
        return content;
    }
}
