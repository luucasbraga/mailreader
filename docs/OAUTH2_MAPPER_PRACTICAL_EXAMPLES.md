# 🔧 OAuth2ProviderMapper - Exemplos Práticos de Uso

## 📍 Pontos Identificados no Código Atual

Após análise do código do projeto MailReader, aqui estão os **pontos específicos** onde você deve usar o `OAuth2ProviderMapper`:

---

## 1. OAuth2Controller - Flexibilizar Path Variable

### 🎯 Problema Atual

O `OAuth2Controller` aceita apenas nomenclatura OAuth2:

```java
// ✅ Funciona: /api/v1/oauth2/microsoft/authorize/123
// ❌ Não funciona: /api/v1/oauth2/outlook/authorize/123
```

### ✅ Solução com Mapper

Adicione validação e conversão no início dos métodos:

```java
@GetMapping("/{provider}/authorize/{emailSearchConfigId}")
public RedirectView authorize(
        @PathVariable String provider,  // Aceita "microsoft" OU "outlook"
        @PathVariable Long emailSearchConfigId
) {
    log.info("Iniciando fluxo OAuth2 para provedor '{}' e EmailSearchConfig ID: {}",
            provider, emailSearchConfigId);

    try {
        // ✅ USAR MAPPER: Normalizar para OAuth2 provider
        String oauth2Provider = normalizeToOAuth2Provider(provider);

        String authorizationUrl = oauth2Service.generateAuthorizationUrl(
            emailSearchConfigId,
            oauth2Provider
        );

        log.info("Redirecionando para {} OAuth2: {}", oauth2Provider, authorizationUrl);
        return new RedirectView(authorizationUrl);

    } catch (IllegalArgumentException e) {
        log.error("Provedor OAuth2 inválido '{}': {}", provider, e.getMessage());
        return new RedirectView("/oauth2/error?message=Provedor+OAuth2+invalido:+" + provider);
    }
}

/**
 * Normaliza provider para nomenclatura OAuth2.
 * Aceita tanto "outlook" quanto "microsoft".
 */
private String normalizeToOAuth2Provider(String provider) {
    // Tenta usar como OAuth2 provider diretamente
    if (oauth2Service.getAvailableProviders().contains(provider.toLowerCase())) {
        return provider.toLowerCase();
    }

    // Se não encontrou, tenta converter de email provider
    try {
        return OAuth2ProviderMapper.toOAuth2Provider(provider);
    } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Provider '" + provider + "' não é válido. " +
            "Use: " + oauth2Service.getAvailableProviders()
        );
    }
}
```

**Benefício:** Usuários podem usar ambos:
- `/api/v1/oauth2/outlook/authorize/123` (mais intuitivo)
- `/api/v1/oauth2/microsoft/authorize/123` (técnico)

---

## 2. Endpoint de Status com Nomenclatura Amigável

### 🎯 Necessidade

Frontend precisa exibir "Conectado com Outlook" para usuários.

### ✅ Implementação

Modifique o método `getOAuthStatus` em `OAuth2Controller`:

```java
@GetMapping("/{provider}/status/{emailSearchConfigId}")
@ResponseBody
public ResponseEntity<OAuthStatusResponse> getOAuthStatus(
        @PathVariable String provider,
        @PathVariable Long emailSearchConfigId
) {
    log.debug("Verificando status OAuth2 para provedor '{}' e EmailSearchConfig ID: {}",
            provider, emailSearchConfigId);

    try {
        // Busca configuração
        EmailSearchConfig config = emailSearchConfigRepository
            .findById(emailSearchConfigId)
            .orElseThrow(() -> new IllegalArgumentException(
                "EmailSearchConfig não encontrado: " + emailSearchConfigId
            ));

        boolean authorized = config.getOauth2Enabled() != null && config.getOauth2Enabled();

        // ✅ USAR MAPPER: Converter para nome amigável
        String displayName = authorized && config.getOauth2Provider() != null
            ? getDisplayProviderName(config.getOauth2Provider())
            : null;

        return ResponseEntity.ok(new OAuthStatusResponse(
            authorized,
            authorized ? "Autorizado" : "Não autorizado",
            displayName  // "Outlook" ao invés de "microsoft"
        ));

    } catch (Exception e) {
        log.error("Erro ao verificar status OAuth2: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(new OAuthStatusResponse(false, "Erro: " + e.getMessage(), null));
    }
}

/**
 * Converte OAuth2 provider para nome de exibição amigável.
 */
private String getDisplayProviderName(String oauth2Provider) {
    try {
        String emailProvider = OAuth2ProviderMapper.toEmailProvider(oauth2Provider);
        // Capitalizar primeira letra
        return emailProvider.substring(0, 1).toUpperCase() + emailProvider.substring(1);
        // "microsoft" → "outlook" → "Outlook"
    } catch (IllegalArgumentException e) {
        // Se não houver mapeamento, retorna o próprio provider
        return oauth2Provider;
    }
}

// Atualizar record para incluir display name
public record OAuthStatusResponse(
        boolean authorized,
        String message,
        String displayProviderName  // "Outlook", "Gmail", etc.
) {}
```

---

## 3. Novo Endpoint: Setup OAuth2 por Email Provider

### 🎯 Caso de Uso

Frontend envia provedor de email (da tabela `tb_email_config`) e sistema inicia OAuth2.

### ✅ Implementação

Criar novo endpoint no `OAuth2Controller`:

```java
/**
 * Endpoint simplificado para iniciar OAuth2 usando nomenclatura de email provider.
 *
 * Útil quando frontend trabalha com tb_email_config.provider ("outlook", "gmail").
 *
 * @param emailProvider Nome do provedor de email (ex: "outlook", "gmail")
 * @param emailSearchConfigId ID da configuração
 * @return Redirect para autorização OAuth2
 */
@GetMapping("/setup/{emailProvider}/authorize/{emailSearchConfigId}")
public RedirectView setupOAuth2(
        @PathVariable String emailProvider,  // "outlook", "gmail"
        @PathVariable Long emailSearchConfigId
) {
    log.info("Setup OAuth2 para email provider '{}' e config ID: {}",
            emailProvider, emailSearchConfigId);

    try {
        // ✅ USAR MAPPER: Validar suporte OAuth2
        if (!OAuth2ProviderMapper.hasOAuth2Support(emailProvider)) {
            log.error("Email provider '{}' não suporta OAuth2", emailProvider);
            return new RedirectView(
                "/oauth2/error?message=Provider+" + emailProvider + "+nao+suporta+OAuth2"
            );
        }

        // ✅ USAR MAPPER: Converter para OAuth2 provider
        String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
        log.debug("Email provider '{}' → OAuth2 provider '{}'", emailProvider, oauth2Provider);

        // Gerar URL de autorização
        String authUrl = oauth2Service.generateAuthorizationUrl(
            emailSearchConfigId,
            oauth2Provider
        );

        return new RedirectView(authUrl);

    } catch (IllegalArgumentException e) {
        log.error("Erro ao converter provider: {}", e.getMessage());
        return new RedirectView("/oauth2/error?message=" + e.getMessage());
    }
}
```

**Uso:**
```bash
# Frontend envia:
GET /api/v1/oauth2/setup/outlook/authorize/123

# Internamente converte "outlook" → "microsoft" e redireciona
```

---

## 4. DTO de Resposta com Ambas Nomenclaturas

### 🎯 Necessidade

API responses devem incluir ambas nomenclaturas para flexibilidade.

### ✅ Implementação

Criar DTO rico com informações de ambos contextos:

```java
package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com informações OAuth2 incluindo ambas nomenclaturas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2StatusDTO {

    @JsonProperty("email")
    private String email;

    @JsonProperty("oauth2_enabled")
    private Boolean oauth2Enabled;

    /**
     * Nome técnico do provedor OAuth2 (ex: "microsoft", "google").
     * Usado internamente pelo sistema.
     */
    @JsonProperty("oauth2_provider")
    private String oauth2Provider;

    /**
     * Nome amigável do provedor de email (ex: "outlook", "gmail").
     * Usado para exibição no frontend.
     */
    @JsonProperty("email_provider")
    private String emailProvider;

    /**
     * Nome de exibição para usuários (ex: "Outlook", "Gmail").
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * Factory method para criar DTO a partir de EmailSearchConfig.
     */
    public static OAuth2StatusDTO fromConfig(EmailSearchConfig config) {
        String oauth2Provider = config.getOauth2Provider();
        String emailProvider = null;
        String displayName = null;

        // ✅ USAR MAPPER para preencher campos
        if (oauth2Provider != null) {
            try {
                emailProvider = OAuth2ProviderMapper.toEmailProvider(oauth2Provider);
                displayName = capitalize(emailProvider);
            } catch (IllegalArgumentException e) {
                // Se não houver mapeamento, usa o próprio oauth2Provider
                emailProvider = oauth2Provider;
                displayName = capitalize(oauth2Provider);
            }
        }

        return OAuth2StatusDTO.builder()
            .email(config.getEmail())
            .oauth2Enabled(config.getOauth2Enabled())
            .oauth2Provider(oauth2Provider)
            .emailProvider(emailProvider)
            .displayName(displayName)
            .build();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
```

**Response JSON:**
```json
{
  "email": "user@outlook.com",
  "oauth2_enabled": true,
  "oauth2_provider": "microsoft",
  "email_provider": "outlook",
  "display_name": "Outlook"
}
```

---

## 5. Service Layer: Validação de Suporte OAuth2

### 🎯 Caso de Uso

Validar se um provedor suporta OAuth2 antes de tentar configurar.

### ✅ Implementação

Adicionar método em um serviço (ex: `EmailConfigService`):

```java
@Service
public class EmailConfigService {

    /**
     * Valida se um email provider suporta OAuth2.
     *
     * @param emailProvider Nome do provedor (ex: "outlook", "gmail")
     * @return true se suporta OAuth2
     */
    public boolean supportsOAuth2(String emailProvider) {
        return OAuth2ProviderMapper.hasOAuth2Support(emailProvider);
    }

    /**
     * Obtém o nome do OAuth2 provider correspondente.
     *
     * @param emailProvider Nome do provedor de email
     * @return Nome do OAuth2 provider
     * @throws IllegalArgumentException se não suporta OAuth2
     */
    public String getOAuth2ProviderName(String emailProvider) {
        if (!supportsOAuth2(emailProvider)) {
            throw new IllegalArgumentException(
                "Email provider '" + emailProvider + "' não suporta OAuth2"
            );
        }

        return OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
    }

    /**
     * Cria configuração de email com OAuth2.
     */
    public EmailSearchConfig setupOAuth2Config(
        String emailProvider,
        String email,
        Company company
    ) {
        // Validar suporte OAuth2
        if (!supportsOAuth2(emailProvider)) {
            throw new IllegalArgumentException(
                "Provider " + emailProvider + " não suporta OAuth2. " +
                "Use autenticação tradicional."
            );
        }

        // Criar configuração
        EmailSearchConfig config = new EmailSearchConfig();
        config.setEmail(email);
        config.setCompany(company);
        config.setOauth2Enabled(false); // Será true após autorização
        config.setActive(true);

        // ✅ USAR MAPPER: Pré-configurar oauth2_provider
        String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
        config.setOauth2Provider(oauth2Provider);

        return emailSearchConfigRepository.save(config);
    }
}
```

---

## 6. Logging e Debugging

### 🎯 Caso de Uso

Logs mais claros mostrando ambas nomenclaturas.

### ✅ Exemplo

```java
@Service
public class OAuth2DebugService {

    public void logProviderMapping(String input) {
        // Descobrir tipo de input
        boolean isOAuth2Provider = OAuth2ProviderMapper.isOAuth2ProviderMapped(input);
        boolean isEmailProvider = OAuth2ProviderMapper.hasOAuth2Support(input);

        if (isOAuth2Provider) {
            String emailProvider = OAuth2ProviderMapper.toEmailProvider(input);
            log.info("OAuth2 Provider: '{}' → Email Provider: '{}'",
                    input, emailProvider);
        }

        if (isEmailProvider) {
            String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(input);
            log.info("Email Provider: '{}' → OAuth2 Provider: '{}'",
                    input, oauth2Provider);
        }

        if (!isOAuth2Provider && !isEmailProvider) {
            log.warn("Provider '{}' não reconhecido", input);
        }
    }
}
```

---

## 📋 Checklist de Implementação

### Prioridade Alta (Implementar agora)

- [x] OAuth2ProviderMapper criado
- [ ] Adicionar `normalizeToOAuth2Provider()` no OAuth2Controller
- [ ] Modificar `getOAuthStatus()` para retornar nome amigável
- [ ] Criar `OAuth2StatusDTO` com ambas nomenclaturas

### Prioridade Média (Próxima sprint)

- [ ] Criar endpoint `/setup/{emailProvider}/authorize/{id}`
- [ ] Adicionar validação `hasOAuth2Support()` em controllers
- [ ] Documentar API com exemplos de ambas nomenclaturas

### Prioridade Baixa (Nice to have)

- [ ] Adicionar endpoint `/api/v1/oauth2/providers/mapping` que retorna mapeamento completo
- [ ] Criar testes unitários para todas conversões
- [ ] Adicionar métricas de qual nomenclatura é mais usada

---

## 🧪 Testes Sugeridos

```java
@Test
public void testOAuth2ControllerAcceptsBothNomenclatures() {
    // Testa "microsoft"
    mockMvc.perform(get("/api/v1/oauth2/microsoft/authorize/123"))
           .andExpect(status().is3xxRedirection());

    // Testa "outlook"
    mockMvc.perform(get("/api/v1/oauth2/outlook/authorize/123"))
           .andExpect(status().is3xxRedirection());

    // Ambos devem funcionar
}

@Test
public void testStatusReturnsDisplayName() {
    EmailSearchConfig config = createConfig();
    config.setOauth2Provider("microsoft");
    config.setOauth2Enabled(true);

    OAuth2StatusDTO dto = OAuth2StatusDTO.fromConfig(config);

    assertEquals("microsoft", dto.getOauth2Provider());
    assertEquals("outlook", dto.getEmailProvider());
    assertEquals("Outlook", dto.getDisplayName());
}
```

---

## 🎯 Resumo Final

| Local | Usar Mapper? | Método | Prioridade |
|-------|--------------|--------|------------|
| OAuth2Controller.authorize() | ✅ SIM | `normalizeToOAuth2Provider()` | 🔴 Alta |
| OAuth2Controller.getOAuthStatus() | ✅ SIM | `toEmailProvider()` | 🔴 Alta |
| OAuth2StatusDTO.fromConfig() | ✅ SIM | `toEmailProvider()` | 🔴 Alta |
| EmailConfigService | ✅ SIM | `hasOAuth2Support()` | 🟡 Média |
| Novo endpoint /setup | ✅ SIM | `toOAuth2Provider()` | 🟡 Média |

---

**Próximo passo:** Implementar modificações de Prioridade Alta no OAuth2Controller.

**Data:** 2026-01-07
