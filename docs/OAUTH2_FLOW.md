# OAuth2 Multi-Provider - Guia Completo

**Versão:** 2.1.0
**Data:** 2026-01-07
**Autor:** MailReader Development Team

---

## 📚 Índice

1. [Introdução](#introdução)
2. [Arquitetura Strategy Pattern](#arquitetura-strategy-pattern)
3. [Nomenclatura e Decisões](#nomenclatura-e-decisões)
4. [OAuth2ProviderMapper](#oauth2providermapper)
5. [Exemplos Práticos](#exemplos-práticos)
6. [Fluxos Completos](#fluxos-completos)
7. [Implementação de Novos Provedores](#implementação-de-novos-provedores)
8. [Troubleshooting](#troubleshooting)

---

## Introdução

### 🎯 Objetivo

O MailReader implementa uma arquitetura OAuth2 multi-provedor usando **Strategy Pattern**, permitindo adicionar novos provedores (Google, Yahoo, etc.) sem modificar código existente.

### ✨ Principais Características

- ✅ **Extensível** - Adicionar novo provedor = criar nova Strategy
- ✅ **Manutenível** - Cada provedor isolado em sua própria classe
- ✅ **Testável** - Strategies independentes facilitam testes
- ✅ **Limpo** - Zero lógica condicional `if (provider == "microsoft")`
- ✅ **Backward Compatible** - Implementação Microsoft anterior continua funcionando

### 🏗️ Componentes Principais

| Componente | Propósito |
|------------|-----------|
| **OAuth2Strategy** | Interface base para todos os provedores |
| **OAuth2StrategyFactory** | Seleciona strategy correta dinamicamente |
| **OAuth2Service** | Facade simplificando integração |
| **MicrosoftOAuth2Strategy** | Implementação para Microsoft |
| **OAuth2Controller** | Controller unificado para todos os provedores |
| **OAuth2ProviderMapper** | Conversão entre nomenclaturas |

---

## Arquitetura Strategy Pattern

### 🎨 Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────┐
│                   OAuth2 Architecture                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  OAuth2Controller                                       │
│         │                                               │
│         ├─> OAuth2Service (Facade)                      │
│         │         │                                     │
│         │         ├─> OAuth2StrategyFactory             │
│         │         │         │                           │
│         │         │         ├─> MicrosoftOAuth2Strategy │
│         │         │         ├─> GoogleOAuth2Strategy    │
│         │         │         └─> YahooOAuth2Strategy     │
│         │         │                                     │
│         │         └─> EmailSearchConfigRepository       │
│         │                                               │
│         └─> OAuth2ProviderMapper (opcional)             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 📦 OAuth2Strategy (Interface)

**Localização:** `service/OAuth2Strategy.java`

```java
public interface OAuth2Strategy {
    String getProviderName();
    String generateAuthorizationUrl(Long emailSearchConfigId);
    void exchangeCodeForTokens(String code, String state);
    String refreshAccessToken(EmailSearchConfig config);
    boolean isTokenExpired(EmailSearchConfig config);
    String getValidAccessToken(EmailSearchConfig config);
    boolean supportsEmailDomain(String email);
}
```

**Responsabilidades:**
- Definir contrato para implementações OAuth2
- Garantir consistência entre provedores
- Permitir polimorfismo via Factory

### 🏭 OAuth2StrategyFactory

**Localização:** `service/OAuth2StrategyFactory.java`

```java
@Service
public class OAuth2StrategyFactory {

    private final Map<String, OAuth2Strategy> strategies;

    @Autowired
    public OAuth2StrategyFactory(List<OAuth2Strategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                OAuth2Strategy::getProviderName,
                Function.identity()
            ));
    }

    public OAuth2Strategy getStrategy(String providerName) {
        // Retorna strategy baseado no nome
    }

    public OAuth2Strategy getStrategyForEmail(String email) {
        // Detecta strategy baseado no domínio do email
    }
}
```

**Responsabilidades:**
- Auto-detectar todas as strategies via Spring DI
- Selecionar strategy por nome ou email
- Validar disponibilidade de provedores

### 🎭 OAuth2Service (Facade)

**Localização:** `service/OAuth2Service.java`

```java
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final OAuth2StrategyFactory strategyFactory;

    public String generateAuthorizationUrl(Long id, String provider) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(provider);
        return strategy.generateAuthorizationUrl(id);
    }

    public String getValidAccessToken(EmailSearchConfig config) {
        String provider = detectProvider(config);
        OAuth2Strategy strategy = strategyFactory.getStrategy(provider);
        return strategy.getValidAccessToken(config);
    }

    private String detectProvider(EmailSearchConfig config) {
        // Prioridade 1: Campo oauth2_provider
        if (config.getOauth2Provider() != null) {
            return config.getOauth2Provider();
        }

        // Prioridade 2: Domínio do email
        OAuth2Strategy strategy = strategyFactory.getStrategyForEmail(
            config.getEmail()
        );
        return strategy.getProviderName();
    }
}
```

**Responsabilidades:**
- Simplificar API para clientes (EmailServiceImpl)
- Delegar operações para strategy correta
- Detectar provedor automaticamente
- Logging unificado

### 🔷 MicrosoftOAuth2Strategy

**Localização:** `service/impl/MicrosoftOAuth2Strategy.java`

```java
@Service("microsoftOAuth2Strategy")
@RequiredArgsConstructor
public class MicrosoftOAuth2Strategy implements OAuth2Strategy {

    private static final String PROVIDER_NAME = "microsoft";
    private static final Set<String> PERSONAL_DOMAINS = Set.of(
        "outlook.com", "hotmail.com", "live.com", "msn.com"
    );

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsEmailDomain(String email) {
        String domain = extractDomain(email);

        // Domínios pessoais Microsoft
        if (PERSONAL_DOMAINS.contains(domain)) {
            return true;
        }

        // Microsoft 365 corporativo (via API UserRealm)
        return isManagedMicrosoftDomain(email);
    }

    private boolean isManagedMicrosoftDomain(String email) {
        String url = "https://login.microsoftonline.com/common/UserRealm/"
                   + email + "?api-version=1.0";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        String accountType = response.get("account_type").asText();

        // "managed" = Microsoft 365 (Azure AD)
        // "federated" = Federado com Microsoft
        return "managed".equals(accountType) || "federated".equals(accountType);
    }

    // Implementar outros métodos...
}
```

**Características:**
- Suporta contas pessoais Microsoft
- Suporta Microsoft 365 corporativo
- Detecção automática via API UserRealm
- Salva `oauth2_provider="microsoft"` no banco

### 🌐 OAuth2Controller (Unificado)

**Localização:** `controller/OAuth2Controller.java`

```java
@Controller
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    /**
     * GET /api/v1/oauth2/{provider}/authorize/{emailSearchConfigId}
     *
     * Exemplos:
     * - /api/v1/oauth2/microsoft/authorize/123
     * - /api/v1/oauth2/google/authorize/456
     */
    @GetMapping("/{provider}/authorize/{emailSearchConfigId}")
    public RedirectView authorize(
        @PathVariable String provider,
        @PathVariable Long emailSearchConfigId
    ) {
        String authUrl = oauth2Service.generateAuthorizationUrl(
            emailSearchConfigId,
            provider
        );
        return new RedirectView(authUrl);
    }

    /**
     * GET /api/v1/oauth2/{provider}/callback
     */
    @GetMapping("/{provider}/callback")
    public RedirectView callback(
        @PathVariable String provider,
        @RequestParam String code,
        @RequestParam String state
    ) {
        oauth2Service.exchangeCodeForTokens(code, state, provider);
        return new RedirectView("/oauth2/success?emailSearchConfigId=" + state);
    }

    /**
     * GET /api/v1/oauth2/providers
     */
    @GetMapping("/providers")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableProviders() {
        return ResponseEntity.ok(oauth2Service.getAvailableProviders());
    }
}
```

**Características:**
- Path variable `{provider}` permite qualquer provedor
- Endpoints RESTful consistentes
- Suporte a múltiplos provedores via um único controller

---

## Nomenclatura e Decisões

### 📋 Contexto

O MailReader possui **duas nomenclaturas** para identificar provedores:

1. **`tb_email_config.provider`** - Serviços de email (IMAP/SMTP)
2. **`oauth2_provider`** - Plataformas OAuth2

### 🎯 Decisão: Manter Nomenclaturas Separadas

**Status:** ✅ APROVADO
**Data:** 2026-01-07

### 🗺️ Mapeamento de Nomenclaturas

| Email Provider<br>(tb_email_config.provider) | OAuth2 Provider<br>(oauth2_provider) | Motivo |
|----------------------------------------------|--------------------------------------|---------|
| `outlook` | `microsoft` | Plataforma Microsoft Identity abrange Outlook + Microsoft 365 |
| `gmail` | `google` | Plataforma Google OAuth2 abrange Gmail + Workspace |
| `yahoo` | `yahoo` | Mesmo nome em ambos contextos |
| `zoho` | `zoho` | Mesmo nome em ambos contextos |
| `icloud` | `apple` | Plataforma Apple OAuth2 |
| `aol` | `aol` | Mesmo nome em ambos contextos |

### ✅ Por Que Manter Separado?

#### 1. Semântica Correta
- `tb_email_config.provider = 'outlook'` → Serviço de email Outlook
- `oauth2_provider = 'microsoft'` → Microsoft Identity Platform
- Microsoft Identity Platform ≠ Outlook (abrange mais serviços)

#### 2. Evita Breaking Changes
- Código existente que busca `provider = 'outlook'` continua funcionando
- Frontend/UI que mostra "Outlook" não precisa mudar
- Queries SQL existentes não quebram

#### 3. Clareza Técnica
- `imap.outlook.com` é específico do Outlook
- Azure AD/Microsoft 365 são mais abrangentes
- Separação reflete diferentes contextos

#### 4. Flexibilidade Futura
- Podemos ter múltiplos provedores OAuth2 para um mesmo serviço
- Ex: Microsoft pode ter OAuth2 corporativo e pessoal separados

### 🗄️ Estrutura de Dados

#### tb_email_config (Configurações de Servidor)

```
id | provider | imap_host           | imap_port | smtp_host           | smtp_port
---|----------|---------------------|-----------|---------------------|----------
1  | gmail    | imap.gmail.com      | 993       | smtp.gmail.com      | 587
2  | outlook  | imap.outlook.com    | 993       | smtp.office365.com  | 587
3  | yahoo    | imap.mail.yahoo.com | 993       | smtp.mail.yahoo.com | 587
```

**Uso:** Quando usuário escolhe provedor, sistema busca configurações IMAP/SMTP.

#### tb_email_search_config (Configurações OAuth2)

```
id | email              | oauth2_enabled | oauth2_provider | oauth2_access_token
---|--------------------|-----------------|-----------------|-----------------------
1  | user@outlook.com   | true            | microsoft       | eyJ0eXAiOiJKV1QiLC...
2  | user@gmail.com     | true            | google          | ya29.a0AfH6SMBx7...
3  | user@example.com   | true            | microsoft       | eyJ0eXAiOiJKV1QiLC...
```

**Uso:** Sistema detecta `oauth2_provider` e chama strategy correspondente.

### 📊 Migração Liquibase

**Arquivo:** `20260105130000_add_oauth2_provider_column.xml`

```xml
<changeSet id="20260105130000-1" author="claude">
    <addColumn tableName="tb_email_search_config">
        <column name="oauth2_provider" type="varchar(50)">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>

<changeSet id="20260105130000-2" author="claude">
    <comment>Popula oauth2_provider='microsoft' para registros existentes</comment>
    <sql>
        UPDATE tb_email_search_config
        SET oauth2_provider = 'microsoft'
        WHERE oauth2_enabled = true
          AND oauth2_access_token IS NOT NULL
          AND oauth2_provider IS NULL;
    </sql>
</changeSet>
```

**Justificativa:** Registros existentes com OAuth2 eram todos Microsoft.

---

## OAuth2ProviderMapper

### 🔧 Propósito

Utilitário **opcional** para converter entre nomenclaturas de email provider e OAuth2 provider.

**Localização:** `util/OAuth2ProviderMapper.java`

### 📝 Quando Usar o Mapper?

| Cenário | Usar Mapper? | Método |
|---------|--------------|--------|
| Strategy salva oauth2_provider | ❌ NÃO | Usa `getProviderName()` |
| Frontend → Backend (iniciar OAuth2) | ✅ SIM | `toOAuth2Provider("outlook")` |
| Backend → Frontend (exibir status) | ✅ SIM | `toEmailProvider("microsoft")` |
| OAuth2 → IMAP/SMTP config | ✅ SIM | `toEmailProvider("microsoft")` |
| Detecção automática de provedor | ❌ NÃO | Usa `strategyFactory.getStrategyForEmail()` |
| Validar suporte OAuth2 | ✅ SIM | `hasOAuth2Support("outlook")` |

### 🎯 API do Mapper

```java
public class OAuth2ProviderMapper {

    /**
     * Converte email provider → OAuth2 provider
     */
    public static String toOAuth2Provider(String emailProvider) {
        // "outlook" → "microsoft"
        // "gmail" → "google"
    }

    /**
     * Converte OAuth2 provider → email provider
     */
    public static String toEmailProvider(String oauth2Provider) {
        // "microsoft" → "outlook"
        // "google" → "gmail"
    }

    /**
     * Verifica se email provider tem suporte OAuth2
     */
    public static boolean hasOAuth2Support(String emailProvider) {
        // Retorna true para "outlook", "gmail", etc.
    }

    /**
     * Verifica se OAuth2 provider está mapeado
     */
    public static boolean isOAuth2ProviderMapped(String oauth2Provider) {
        // Retorna true para "microsoft", "google", etc.
    }
}
```

### ⚠️ Importante: O Mapper é OPCIONAL

O sistema funciona **100% sem o mapper**:
- ✅ OAuth2Strategy Pattern está completo
- ✅ Todas as operações OAuth2 funcionam
- ✅ Nenhum erro de lógica ocorre sem ele

**Use o mapper apenas quando precisar:**
- Converter nomenclaturas entre fronteiras do sistema
- API mais flexível (aceitar "outlook" OU "microsoft")
- Exibir nomes amigáveis no frontend

---

## Exemplos Práticos

### 1️⃣ Controller Aceitando Ambas Nomenclaturas

```java
@GetMapping("/{provider}/authorize/{emailSearchConfigId}")
public RedirectView authorize(@PathVariable String provider, ...) {
    // ✅ USAR MAPPER: Normalizar input
    String oauth2Provider = normalizeToOAuth2Provider(provider);
    // Aceita "outlook" OU "microsoft"

    String authUrl = oauth2Service.generateAuthorizationUrl(id, oauth2Provider);
    return new RedirectView(authUrl);
}

private String normalizeToOAuth2Provider(String provider) {
    // Tenta usar como OAuth2 provider diretamente
    if (oauth2Service.getAvailableProviders().contains(provider.toLowerCase())) {
        return provider.toLowerCase();
    }

    // Se não encontrou, converte de email provider
    return OAuth2ProviderMapper.toOAuth2Provider(provider);
}
```

**Benefício:** Usuários podem usar:
- `/api/v1/oauth2/outlook/authorize/123` ✅
- `/api/v1/oauth2/microsoft/authorize/123` ✅

### 2️⃣ DTO com Nomenclatura Amigável

```java
@Data
@Builder
public class OAuth2StatusDTO {
    private String email;
    private Boolean oauth2Enabled;

    // Técnico (interno)
    private String oauth2Provider;    // "microsoft"

    // Amigável (UI)
    private String emailProvider;     // "outlook"
    private String displayName;       // "Outlook"

    public static OAuth2StatusDTO fromConfig(EmailSearchConfig config) {
        String oauth2Prov = config.getOauth2Provider();
        String emailProv = OAuth2ProviderMapper.toEmailProvider(oauth2Prov);

        return OAuth2StatusDTO.builder()
            .oauth2Provider(oauth2Prov)
            .emailProvider(emailProv)
            .displayName(capitalize(emailProv))
            .build();
    }
}
```

**Response JSON:**
```json
{
  "oauth2_provider": "microsoft",
  "email_provider": "outlook",
  "display_name": "Outlook"
}
```

### 3️⃣ Validação de Suporte OAuth2

```java
@PostMapping("/email-config/setup-oauth2")
public ResponseEntity<?> setupOAuth2(@RequestParam String provider) {
    // ✅ USAR MAPPER: Validar suporte
    if (!OAuth2ProviderMapper.hasOAuth2Support(provider)) {
        return ResponseEntity.badRequest()
            .body("Provider " + provider + " não suporta OAuth2");
    }

    String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(provider);
    // Continuar com setup...
}
```

### 4️⃣ Endpoint Simplificado com Email Provider

```java
@GetMapping("/setup/{emailProvider}/authorize/{id}")
public RedirectView setupOAuth2(
    @PathVariable String emailProvider  // "outlook", "gmail"
) {
    // ✅ USAR MAPPER: Converter e validar
    if (!OAuth2ProviderMapper.hasOAuth2Support(emailProvider)) {
        return new RedirectView("/oauth2/error?message=Provider+nao+suporta+OAuth2");
    }

    String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
    String authUrl = oauth2Service.generateAuthorizationUrl(id, oauth2Provider);

    return new RedirectView(authUrl);
}
```

---

## Fluxos Completos

### 🔄 Fluxo de Autorização (Authorization Code Flow)

```
┌─────────────────────────────────────────────────────────────┐
│                   AUTHORIZATION FLOW                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Usuário clica "Conectar com Outlook"                    │
│     Frontend → GET /api/v1/oauth2/microsoft/authorize/123   │
│                                                             │
│  2. OAuth2Controller.authorize()                            │
│     │                                                       │
│     ├─> OAuth2Service.generateAuthorizationUrl()           │
│     │         │                                             │
│     │         ├─> OAuth2StrategyFactory.getStrategy("microsoft") │
│     │         │                                             │
│     │         └─> MicrosoftOAuth2Strategy.generateAuthorizationUrl() │
│     │                                                       │
│     └─> RedirectView(https://login.microsoftonline.com/...) │
│                                                             │
│  3. Browser redireciona para Microsoft                      │
│     Usuário faz login e autoriza permissões                 │
│                                                             │
│  4. Microsoft redireciona de volta                          │
│     → /api/v1/oauth2/microsoft/callback?code=ABC&state=123  │
│                                                             │
│  5. OAuth2Controller.callback()                             │
│     │                                                       │
│     ├─> OAuth2Service.exchangeCodeForTokens()              │
│     │         │                                             │
│     │         ├─> MicrosoftOAuth2Strategy.exchangeCodeForTokens() │
│     │         │         │                                   │
│     │         │         ├─> POST to Microsoft token endpoint │
│     │         │         │                                   │
│     │         │         ├─> Recebe: access_token, refresh_token │
│     │         │         │                                   │
│     │         │         └─> Salva no banco:                 │
│     │         │                oauth2_enabled = true        │
│     │         │                oauth2_provider = "microsoft" │
│     │         │                oauth2_access_token = "..."  │
│     │         │                oauth2_refresh_token = "..." │
│     │         │                oauth2_token_expiry = ...    │
│     │         │                                             │
│     │         └─> EmailSearchConfigRepository.save()        │
│     │                                                       │
│     └─> RedirectView("/oauth2/success")                    │
│                                                             │
│  6. Usuário vê página de sucesso                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 🔄 Fluxo de Uso (Email Reading)

```
┌─────────────────────────────────────────────────────────────┐
│                    EMAIL READING FLOW                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Sistema precisa ler emails                              │
│     EmailServiceImpl.connectToEmailStore()                  │
│     │                                                       │
│     ├─> if (config.getOauth2Enabled())                     │
│     │         │                                             │
│     │         ├─> OAuth2Service.getValidAccessToken(config) │
│     │         │         │                                   │
│     │         │         ├─> detectProvider(config)          │
│     │         │         │     ├─ Prioridade 1: oauth2_provider do banco │
│     │         │         │     └─ Prioridade 2: Domínio do email │
│     │         │         │                                   │
│     │         │         ├─> OAuth2StrategyFactory.getStrategy("microsoft") │
│     │         │         │                                   │
│     │         │         ├─> MicrosoftOAuth2Strategy.getValidAccessToken() │
│     │         │         │         │                         │
│     │         │         │         ├─> if (isTokenExpired()) │
│     │         │         │         │     │                   │
│     │         │         │         │     └─> refreshAccessToken() │
│     │         │         │         │           │             │
│     │         │         │         │           ├─> POST to token endpoint │
│     │         │         │         │           │                   │
│     │         │         │         │           ├─> Atualiza tokens │
│     │         │         │         │           │                   │
│     │         │         │         │           └─> Salva no banco  │
│     │         │         │         │                         │
│     │         │         │         └─> return accessToken    │
│     │         │         │                                   │
│     │         │         └─> return accessToken              │
│     │         │                                             │
│     │         └─> password = accessToken                    │
│     │                                                       │
│     ├─> properties.put("mail.imap.auth.mechanisms", "XOAUTH2") │
│     │                                                       │
│     ├─> Session.getInstance(properties)                    │
│     │                                                       │
│     └─> store.connect(email, password=accessToken)         │
│                                                             │
│  2. Sistema lê emails usando XOAUTH2                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 🔄 Fluxo com Mapper (Opcional)

```
┌─────────────────────────────────────────────────────────────┐
│              FLOW WITH MAPPER (OPTIONAL)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Frontend envia: "outlook"                                  │
│         │                                                   │
│         ▼                                                   │
│  OAuth2Controller.authorize(provider="outlook")             │
│         │                                                   │
│         ├─> normalizeToOAuth2Provider("outlook")            │
│         │         │                                         │
│         │         └─> OAuth2ProviderMapper.toOAuth2Provider("outlook") │
│         │                                                   │
│         │           "outlook" → "microsoft"                 │
│         │                                                   │
│         ▼                                                   │
│  OAuth2Service.generateAuthorizationUrl(id, "microsoft")    │
│         │                                                   │
│         ▼                                                   │
│  (Fluxo normal continua com "microsoft")                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementação de Novos Provedores

### 🚀 Como Adicionar Google OAuth2

#### Passo 1: Criar GoogleOAuth2Strategy

```java
package br.com.groupsoftware.grouppay.extratoremail.service.impl;

@Service("googleOAuth2Strategy")
@RequiredArgsConstructor
public class GoogleOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;
    private final EmailSearchConfigRepository repository;

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.redirect-uri}")
    private String redirectUri;

    private static final String PROVIDER_NAME = "google";
    private static final String AUTHORIZATION_ENDPOINT =
        "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT =
        "https://oauth2.googleapis.com/token";
    private static final String SCOPES =
        "https://mail.google.com/ openid email profile";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsEmailDomain(String email) {
        String domain = extractDomain(email);
        return domain.equals("gmail.com") || domain.equals("googlemail.com");
    }

    @Override
    public String generateAuthorizationUrl(Long emailSearchConfigId) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZATION_ENDPOINT)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", SCOPES)
            .queryParam("state", emailSearchConfigId)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .toUriString();
    }

    // Implementar outros métodos (exchangeCodeForTokens, refreshAccessToken, etc.)
}
```

#### Passo 2: Configurar application.yml

```yaml
google:
  oauth2:
    client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
    client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
    redirect-uri: ${GOOGLE_OAUTH2_REDIRECT_URI:http://localhost:8080/api/v1/oauth2/google/callback}
```

#### Passo 3: Configurar Google Cloud Console

1. Acesse [Google Cloud Console](https://console.cloud.google.com)
2. Crie novo projeto ou selecione existente
3. Habilite "Gmail API"
4. Crie credenciais OAuth 2.0:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/api/v1/oauth2/google/callback`
5. Copie Client ID e Client Secret

#### Passo 4: Definir Environment Variables

```bash
export GOOGLE_OAUTH2_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_OAUTH2_CLIENT_SECRET="your-client-secret"
export GOOGLE_OAUTH2_REDIRECT_URI="http://localhost:8080/api/v1/oauth2/google/callback"
```

#### Passo 5: Testar

```bash
# Iniciar autorização
GET http://localhost:8080/api/v1/oauth2/google/authorize/123

# Spring detecta automaticamente GoogleOAuth2Strategy!
# Nenhuma outra mudança necessária
```

### ✅ Checklist para Novo Provedor

- [ ] Criar classe `XxxOAuth2Strategy implements OAuth2Strategy`
- [ ] Anotar com `@Service("xxxOAuth2Strategy")`
- [ ] Implementar `getProviderName()` retornando nome em lowercase
- [ ] Implementar `supportsEmailDomain()` para domínios do provedor
- [ ] Implementar `generateAuthorizationUrl()` com endpoint correto
- [ ] Implementar `exchangeCodeForTokens()` para trocar code por tokens
- [ ] Implementar `refreshAccessToken()` para renovar tokens
- [ ] Implementar `isTokenExpired()` com buffer de 5 minutos
- [ ] Implementar `getValidAccessToken()` com renovação automática
- [ ] Adicionar configurações em `application.yml`
- [ ] Configurar OAuth App no console do provedor
- [ ] Adicionar redirect URI no console do provedor
- [ ] Definir environment variables
- [ ] Testar fluxo completo

### 🎯 Provedores Preparados para Implementação

| Provedor | Strategy Name | Domínios | Authorization Endpoint |
|----------|---------------|----------|------------------------|
| **Google** | `google` | gmail.com, googlemail.com | https://accounts.google.com/o/oauth2/v2/auth |
| **Yahoo** | `yahoo` | yahoo.com, yahoo.com.br | https://api.login.yahoo.com/oauth2/request_auth |
| **Outlook.com** | `microsoft` | outlook.com, hotmail.com, live.com | https://login.microsoftonline.com/.../authorize |

---

## Troubleshooting

### ❌ Erro: "Provider 'outlook' not supported"

**Causa:** Controller recebeu "outlook" mas esperava "microsoft"

**Solução 1 (Recomendada):** Usar nomenclatura OAuth2
```bash
# Ao invés de:
GET /api/v1/oauth2/outlook/authorize/123

# Usar:
GET /api/v1/oauth2/microsoft/authorize/123
```

**Solução 2:** Implementar normalização no controller com mapper
```java
private String normalizeToOAuth2Provider(String provider) {
    if (oauth2Service.getAvailableProviders().contains(provider.toLowerCase())) {
        return provider.toLowerCase();
    }
    return OAuth2ProviderMapper.toOAuth2Provider(provider);
}
```

### ❌ Erro: "Redirect URI mismatch"

**Causa:** URI configurada no Azure AD não corresponde à URI enviada

**Solução:**
1. Verifique `${MICROSOFT_OAUTH2_REDIRECT_URI}` em application.yml
2. Acesse Azure Portal → App Registrations → [Sua App] → Authentication
3. Adicione URI exata em "Redirect URIs"
4. Exemplo: `http://localhost:8080/api/v1/oauth2/microsoft/callback`

**Dica:** Registre múltiplas URIs para diferentes ambientes:
```
✓ http://localhost:8080/api/v1/oauth2/microsoft/callback (Dev)
✓ https://homolog.example.com/api/v1/oauth2/microsoft/callback (Homolog)
✓ https://example.com/api/v1/oauth2/microsoft/callback (Prod)
```

### ❌ Erro: "Token expired" ao ler emails

**Causa:** Access token expirou e refresh falhou

**Diagnóstico:**
```sql
SELECT id, email, oauth2_enabled, oauth2_provider, oauth2_token_expiry
FROM tb_email_search_config
WHERE id = 123;
```

**Solução:**
1. Verificar se `oauth2_refresh_token` existe no banco
2. Verificar logs de erro no `refreshAccessToken()`
3. Se refresh token expirou, usuário precisa autorizar novamente:
   ```bash
   GET /api/v1/oauth2/microsoft/authorize/123
   ```

### ❌ Erro: "No OAuth2 provider found for email"

**Causa:** Nenhuma strategy suporta o domínio do email

**Diagnóstico:**
```java
// Testar detecção
String email = "user@customdomain.com";
try {
    OAuth2Strategy strategy = strategyFactory.getStrategyForEmail(email);
    System.out.println("Detected: " + strategy.getProviderName());
} catch (IllegalArgumentException e) {
    System.out.println("No strategy found for: " + email);
}
```

**Solução:**
1. Verificar se domínio é Microsoft 365:
   - Acessar: `https://login.microsoftonline.com/common/UserRealm/user@customdomain.com?api-version=1.0`
   - Se `account_type == "managed"` → Usar Microsoft strategy
2. Ou definir `oauth2_provider` manualmente no banco:
   ```sql
   UPDATE tb_email_search_config
   SET oauth2_provider = 'microsoft'
   WHERE id = 123;
   ```

### ❌ Erro: "IllegalArgumentException: Provider name cannot be null"

**Causa:** Campo `oauth2_provider` está NULL no banco e detecção automática falhou

**Solução:**
```sql
-- Verificar configuração
SELECT id, email, oauth2_provider, oauth2_enabled
FROM tb_email_search_config
WHERE id = 123;

-- Se oauth2_provider está NULL mas OAuth2 está habilitado:
UPDATE tb_email_search_config
SET oauth2_provider = 'microsoft'
WHERE id = 123 AND oauth2_enabled = true;
```

### 🔍 Debug Checklist

- [ ] Verificar logs do Spring Boot
- [ ] Verificar `oauth2_provider` no banco está correto
- [ ] Verificar tokens não estão expirados
- [ ] Verificar environment variables estão definidas
- [ ] Verificar redirect URI no console do provedor
- [ ] Verificar strategy está registrada no Spring (`@Service`)
- [ ] Verificar nome do provedor está em lowercase

### 📊 Comandos Úteis para Debug

```sql
-- Ver todas configurações OAuth2
SELECT id, email, oauth2_enabled, oauth2_provider,
       oauth2_token_expiry,
       CASE WHEN oauth2_token_expiry < NOW() THEN 'EXPIRED' ELSE 'VALID' END as token_status
FROM tb_email_search_config
WHERE oauth2_enabled = true;

-- Ver configurações de um email específico
SELECT * FROM tb_email_search_config WHERE email = 'user@outlook.com';

-- Resetar OAuth2 para reautorizar
UPDATE tb_email_search_config
SET oauth2_enabled = false,
    oauth2_access_token = NULL,
    oauth2_refresh_token = NULL,
    oauth2_token_expiry = NULL
WHERE id = 123;
```

---

## 📚 Referências

### Documentação Oficial

- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [Microsoft Identity Platform](https://learn.microsoft.com/en-us/azure/active-directory/develop/)
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)

### Design Patterns

- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Factory Pattern](https://refactoring.guru/design-patterns/factory-method)
- [Facade Pattern](https://refactoring.guru/design-patterns/facade)

### Projeto MailReader

- [CHANGELOG.md](../CHANGELOG.md) - Histórico de mudanças
- [OAUTH2_SETUP.md](OAUTH2_SETUP.md) - Guia de configuração OAuth2
- Commits relacionados:
  - `b54aaf0` - refactor: Implement Strategy Pattern for multi-provider OAuth2 support
  - `e5d77b1` - docs: Add OAuth2 provider naming convention and mapper utility
  - `ca59c15` - docs: Add comprehensive OAuth2ProviderMapper usage guide

---

**Versão do Documento:** 2.1.0
**Última Atualização:** 2026-01-07
**Contribuidores:** MailReader Development Team
