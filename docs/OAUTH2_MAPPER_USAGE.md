# 📍 Onde Usar OAuth2ProviderMapper

## 🎯 Identificação dos Pontos de Integração

Após análise do código, aqui estão os **principais locais** onde o `OAuth2ProviderMapper` deve ser usado:

---

## 1️⃣ **MicrosoftOAuth2Strategy.saveTokens()** ✅ JÁ IMPLEMENTADO

**Arquivo:** `MicrosoftOAuth2Strategy.java:290`

**Status:** ✅ Implementado corretamente

```java
private void saveTokens(EmailSearchConfig config, OAuth2TokenResponse tokenResponse) {
    // ...
    config.setOauth2Provider(PROVIDER_NAME); // "microsoft"
    emailSearchConfigRepository.save(config);
}
```

**Uso atual:** Já salva "microsoft" hardcoded.

**Não precisa de mapper aqui** - As strategies devem usar seu próprio `getProviderName()`.

---

## 2️⃣ **Frontend/API: Iniciar Autorização OAuth2** ⚠️ PONTO DE INTEGRAÇÃO

**Cenário:** Usuário seleciona provedor de email no frontend e inicia OAuth2.

### Fluxo Atual (Hipotético)

```java
// Frontend: Usuário seleciona "Outlook" em um dropdown
// POST /api/v1/email-config com { "provider": "outlook", "email": "user@outlook.com" }

// Backend recebe:
String emailProvider = request.getParameter("provider"); // "outlook"

// ❌ PROBLEMA: Controller OAuth2 espera "microsoft", não "outlook"
// GET /api/v1/oauth2/microsoft/authorize/{id} ← Precisa ser "microsoft"
```

### ✅ Solução com Mapper

**Onde usar:** Controller que processa seleção de provedor de email

```java
@PostMapping("/api/v1/email-config/setup-oauth2")
public ResponseEntity<String> setupOAuth2(
    @RequestParam String provider,  // "outlook" (do frontend/tb_email_config)
    @RequestParam Long emailSearchConfigId
) {
    // Converte email provider → OAuth2 provider
    String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(provider);
    // "outlook" → "microsoft"

    // Gera URL de autorização
    String authUrl = oauth2Service.generateAuthorizationUrl(
        emailSearchConfigId,
        oauth2Provider
    );

    return ResponseEntity.ok(authUrl);
}
```

---

## 3️⃣ **Detecção Automática de Provedor (se necessário)** 🔍

**Cenário:** Sistema detecta automaticamente qual provedor OAuth2 usar baseado no email.

### Implementação Atual

**Arquivo:** `OAuth2Service.detectProvider()` (privado)

```java
private String detectProvider(EmailSearchConfig emailSearchConfig) {
    // Prioridade 1: Campo oauth2_provider já configurado
    if (emailSearchConfig.getOauth2Provider() != null && !empty) {
        return emailSearchConfig.getOauth2Provider(); // "microsoft"
    }

    // Prioridade 2: Detectar baseado no domínio do email
    OAuth2Strategy strategy = strategyFactory.getStrategyForEmail(email);
    return strategy.getProviderName(); // "microsoft"
}
```

**Status:** ✅ Funciona bem, usa strategies para detecção.

**Não precisa de mapper** - Strategies já retornam nome OAuth2 correto.

---

## 4️⃣ **UI/Frontend: Exibir Status OAuth2** 🖥️

**Cenário:** Frontend precisa exibir "Conectado com Outlook" ao usuário.

### API Response

```java
@GetMapping("/api/v1/email-config/{id}/status")
public ResponseEntity<EmailConfigStatusDTO> getStatus(@PathVariable Long id) {
    EmailSearchConfig config = repository.findById(id).orElseThrow();

    // ✅ Converter OAuth2 provider → Email provider para UI
    String displayProvider = null;
    if (config.getOauth2Provider() != null) {
        displayProvider = OAuth2ProviderMapper.toEmailProvider(
            config.getOauth2Provider()
        );
        // "microsoft" → "outlook"
    }

    return ResponseEntity.ok(EmailConfigStatusDTO.builder()
        .email(config.getEmail())
        .oauth2Enabled(config.getOauth2Enabled())
        .oauth2Provider(displayProvider) // "outlook" para exibição
        .build());
}
```

**Por quê?** Frontend/usuários entendem "Outlook", não "Microsoft Identity Platform".

---

## 5️⃣ **Integração com tb_email_config** 📊

**Cenário:** Buscar configurações IMAP/SMTP baseado no provedor OAuth2.

### Uso do Mapper

```java
// EmailSearchConfig tem oauth2_provider = "microsoft"
String oauth2Provider = emailSearchConfig.getOauth2Provider(); // "microsoft"

// Converter para buscar configurações IMAP/SMTP
String emailProvider = OAuth2ProviderMapper.toEmailProvider(oauth2Provider);
// "microsoft" → "outlook"

// Buscar na tb_email_config
EmailConfig imapSmtpConfig = emailConfigRepository.findByProvider(emailProvider);
// SELECT * FROM tb_email_config WHERE provider = 'outlook'
// → imap.outlook.com, smtp.office365.com
```

**Útil se:** Você precisar correlacionar OAuth2 com configurações IMAP/SMTP.

---

## 6️⃣ **Migração/Sincronização de Dados** 🔄

**Cenário:** Script para popular oauth2_provider baseado em provider existente.

### Script de Migração (Opcional)

```java
@Service
public class OAuth2ProviderMigrationService {

    public void migrateProviders() {
        List<EmailSearchConfig> configs = repository.findAll();

        for (EmailSearchConfig config : configs) {
            if (config.getOauth2Enabled() && config.getOauth2Provider() == null) {
                // Assumir que veio de tb_email_config.provider
                String emailProvider = detectEmailProviderFromConfig(config);

                if (OAuth2ProviderMapper.hasOAuth2Support(emailProvider)) {
                    String oauth2Provider = OAuth2ProviderMapper
                        .toOAuth2Provider(emailProvider);

                    config.setOauth2Provider(oauth2Provider);
                    repository.save(config);
                }
            }
        }
    }
}
```

---

## 7️⃣ **Validação de Provedor** ✅

**Cenário:** Validar se um email provider tem suporte OAuth2.

### Exemplo de Validação

```java
@PostMapping("/api/v1/email-config/validate-oauth2-support")
public ResponseEntity<Boolean> validateOAuth2Support(@RequestParam String provider) {
    // "outlook", "gmail", "yahoo", etc.
    boolean hasSupport = OAuth2ProviderMapper.hasOAuth2Support(provider);

    return ResponseEntity.ok(hasSupport);
}
```

---

## 📋 Resumo: Quando Usar

| Cenário | Usar Mapper? | Método |
|---------|--------------|--------|
| **Strategy salva oauth2_provider** | ❌ Não | Usa `getProviderName()` |
| **Frontend → Backend (iniciar OAuth2)** | ✅ SIM | `toOAuth2Provider("outlook")` |
| **Backend → Frontend (exibir status)** | ✅ SIM | `toEmailProvider("microsoft")` |
| **OAuth2 → IMAP/SMTP config** | ✅ SIM | `toEmailProvider("microsoft")` |
| **Detecção automática de provedor** | ❌ Não | Usa `strategyFactory.getStrategyForEmail()` |
| **Validar suporte OAuth2** | ✅ SIM | `hasOAuth2Support("outlook")` |
| **Migration scripts** | ✅ SIM | `toOAuth2Provider(emailProvider)` |

---

## 🎯 Principais Casos de Uso Práticos

### Caso 1: Controller de Configuração de Email

```java
@RestController
@RequestMapping("/api/v1/email-config")
public class EmailConfigController {

    @PostMapping("/start-oauth2")
    public RedirectView startOAuth2(
        @RequestParam String provider,  // "outlook" do frontend
        @RequestParam Long configId
    ) {
        // ✅ USAR MAPPER AQUI
        String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(provider);
        String authUrl = oauth2Service.generateAuthorizationUrl(configId, oauth2Provider);
        return new RedirectView(authUrl);
    }
}
```

### Caso 2: DTO para Frontend

```java
@Data
public class EmailConfigStatusDTO {
    private String email;
    private Boolean oauth2Enabled;

    @JsonProperty("provider") // Frontend entende "outlook"
    private String displayProvider; // ← Usar toEmailProvider() aqui

    @JsonProperty("oauth2Provider") // Técnico: "microsoft"
    private String oauth2Provider;
}
```

### Caso 3: Serviço de Configuração

```java
@Service
public class EmailConfigService {

    public void setupOAuth2(String emailProvider, String email) {
        // Validar se suporta OAuth2
        if (!OAuth2ProviderMapper.hasOAuth2Support(emailProvider)) {
            throw new IllegalArgumentException(
                "Provider " + emailProvider + " não suporta OAuth2"
            );
        }

        // Converter e iniciar OAuth2
        String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
        oauth2Service.generateAuthorizationUrl(configId, oauth2Provider);
    }
}
```

---

## ⚠️ Onde NÃO Usar

1. **Dentro das Strategies** - Use `getProviderName()` diretamente
2. **OAuth2Service/Factory** - Já usam nomes OAuth2 corretos
3. **Banco de dados** - oauth2_provider já está normalizado

---

## 🔍 Checklist de Implementação

- [ ] Identificar controllers que recebem `provider` do frontend
- [ ] Usar `toOAuth2Provider()` para converter antes de chamar OAuth2Service
- [ ] Usar `toEmailProvider()` em DTOs de resposta para frontend
- [ ] Adicionar validação com `hasOAuth2Support()` onde relevante
- [ ] Atualizar documentação de API para clarificar nomenclatura

---

**Conclusão:** O mapper é mais útil nas **fronteiras do sistema** (controllers, DTOs, APIs), onde há interação entre o mundo "email provider" (outlook, gmail) e o mundo "OAuth2 provider" (microsoft, google).

**Data:** 2026-01-07
**Versão:** 1.0
