# Prompt: Refatorar OAuth2 com Strategy Pattern

## 📋 Contexto

O projeto MailReader atualmente possui uma implementação de OAuth2 específica para Microsoft (Authorization Code Flow). Esta implementação permite que usuários autorizem o acesso aos seus emails de contas Microsoft (pessoais e comerciais).

No futuro, precisaremos suportar outros provedores OAuth2 como:
- Google (Gmail)
- Yahoo
- Outros provedores de email que exigem OAuth2

## 🎯 Objetivo

Refatorar a implementação atual de OAuth2 para usar **Strategy Pattern**, tornando o código:
- ✅ **Extensível**: Fácil adicionar novos provedores OAuth2
- ✅ **Manutenível**: Cada provedor isolado em sua própria estratégia
- ✅ **Testável**: Strategies independentes facilitam testes unitários
- ✅ **Limpo**: Evitar if/else gigantes baseados no provedor

## 📂 Arquivos Relevantes

### Implementação Atual (Microsoft-specific):

```
src/main/java/br/com/groupsoftware/grouppay/extratoremail/
├── service/
│   ├── MicrosoftOAuth2Service.java
│   └── impl/MicrosoftOAuth2ServiceImpl.java
├── controller/
│   └── MicrosoftOAuth2Controller.java
├── domain/
│   ├── entity/EmailSearchConfig.java
│   └── model/dto/MicrosoftOAuth2TokenResponse.java
```

### Arquivos que Usam OAuth2:

```
src/main/java/br/com/groupsoftware/grouppay/extratoremail/
└── service/impl/EmailServiceImpl.java (usa MicrosoftOAuth2Service)
```

## 🏗️ Estrutura Desejada (Strategy Pattern)

### 1. Interface Base (Strategy Interface)

```java
/**
 * Interface base para estratégias de OAuth2.
 * Cada provedor (Microsoft, Google, Yahoo, etc.) implementará esta interface.
 */
public interface OAuth2Strategy {

    /**
     * Retorna o identificador único do provedor (ex: "microsoft", "google", "yahoo")
     */
    String getProviderName();

    /**
     * Gera URL de autorização para o provedor OAuth2
     */
    String generateAuthorizationUrl(Long emailSearchConfigId);

    /**
     * Troca authorization code por tokens
     */
    void exchangeCodeForTokens(String code, String state);

    /**
     * Renova access token usando refresh token
     */
    String refreshAccessToken(EmailSearchConfig emailSearchConfig);

    /**
     * Verifica se o token está expirado ou próximo da expiração
     */
    boolean isTokenExpired(EmailSearchConfig emailSearchConfig);

    /**
     * Obtém um access token válido (renova se necessário)
     */
    String getValidAccessToken(EmailSearchConfig emailSearchConfig);

    /**
     * Verifica se este provedor suporta o domínio do email
     * Ex: microsoft.com, outlook.com, gmail.com, etc.
     */
    boolean supportsEmailDomain(String email);
}
```

### 2. Implementações Concretas (Concrete Strategies)

```java
/**
 * Implementação OAuth2 para Microsoft
 */
@Service("microsoftOAuth2Strategy")
public class MicrosoftOAuth2Strategy implements OAuth2Strategy {
    // Implementação atual do MicrosoftOAuth2ServiceImpl
    // movida para esta classe
}

/**
 * Implementação OAuth2 para Google (futura)
 */
@Service("googleOAuth2Strategy")
public class GoogleOAuth2Strategy implements OAuth2Strategy {
    // Implementação para Gmail
}

/**
 * Implementação OAuth2 para Yahoo (futura)
 */
@Service("yahooOAuth2Strategy")
public class YahooOAuth2Strategy implements OAuth2Strategy {
    // Implementação para Yahoo
}
```

### 3. Context/Factory (OAuth2StrategyFactory)

```java
/**
 * Factory para obter a estratégia OAuth2 correta baseada no email ou provedor.
 */
@Service
public class OAuth2StrategyFactory {

    private final Map<String, OAuth2Strategy> strategies;

    @Autowired
    public OAuth2StrategyFactory(List<OAuth2Strategy> strategyList) {
        // Popula map com todas as strategies disponíveis
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                OAuth2Strategy::getProviderName,
                Function.identity()
            ));
    }

    /**
     * Retorna estratégia baseada no nome do provedor
     */
    public OAuth2Strategy getStrategy(String providerName) {
        OAuth2Strategy strategy = strategies.get(providerName);
        if (strategy == null) {
            throw new IllegalArgumentException("Provider not supported: " + providerName);
        }
        return strategy;
    }

    /**
     * Detecta e retorna estratégia baseada no email
     */
    public OAuth2Strategy getStrategyForEmail(String email) {
        return strategies.values().stream()
            .filter(strategy -> strategy.supportsEmailDomain(email))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No OAuth2 provider found for email: " + email
            ));
    }
}
```

### 4. Service Facade (OAuth2Service)

```java
/**
 * Service facade que usa a factory para delegar para a estratégia correta.
 * Este é o serviço que será injetado em outros componentes.
 */
@Service
public class OAuth2Service {

    private final OAuth2StrategyFactory strategyFactory;

    @Autowired
    public OAuth2Service(OAuth2StrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public String generateAuthorizationUrl(Long emailSearchConfigId, String providerName) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        return strategy.generateAuthorizationUrl(emailSearchConfigId);
    }

    public void exchangeCodeForTokens(String code, String state, String providerName) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        strategy.exchangeCodeForTokens(code, state);
    }

    public String getValidAccessToken(EmailSearchConfig emailSearchConfig) {
        // Detecta provedor baseado no email ou campo específico
        String providerName = detectProvider(emailSearchConfig);
        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        return strategy.getValidAccessToken(emailSearchConfig);
    }

    private String detectProvider(EmailSearchConfig config) {
        // Lógica para detectar provedor (pode usar campo no banco ou domínio do email)
        return strategyFactory.getStrategyForEmail(config.getEmail()).getProviderName();
    }
}
```

### 5. Controller Unificado

```java
/**
 * Controller unificado para OAuth2 de todos os provedores.
 * Usa path variable para identificar o provedor.
 */
@Controller
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    /**
     * Inicia autorização OAuth2
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
        String authUrl = oauth2Service.generateAuthorizationUrl(emailSearchConfigId, provider);
        return new RedirectView(authUrl);
    }

    /**
     * Callback OAuth2
     * GET /api/v1/oauth2/{provider}/callback
     */
    @GetMapping("/{provider}/callback")
    public RedirectView callback(
        @PathVariable String provider,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        // Implementação similar ao controller atual
        // mas delegando para oauth2Service
    }
}
```

### 6. Atualização de EmailSearchConfig

```java
@Entity
@Table(name = "tb_email_search_config")
public class EmailSearchConfig implements Serializable {

    // Campos existentes...

    @Column(name = "OAUTH2_ENABLED")
    private Boolean oauth2Enabled = false;

    @Column(name = "OAUTH2_PROVIDER")  // NOVO CAMPO
    private String oauth2Provider;  // "microsoft", "google", "yahoo", etc.

    @Column(name = "OAUTH2_ACCESS_TOKEN", columnDefinition = "TEXT")
    private String oauth2AccessToken;

    @Column(name = "OAUTH2_REFRESH_TOKEN", columnDefinition = "TEXT")
    private String oauth2RefreshToken;

    @Column(name = "OAUTH2_TOKEN_EXPIRY")
    private LocalDateTime oauth2TokenExpiry;
}
```

### 7. Configuração (application.yml)

```yaml
# OAuth2 Configuration - Multi-Provider
oauth2:
  providers:
    microsoft:
      client-id: ${MICROSOFT_OAUTH2_CLIENT_ID}
      client-secret: ${MICROSOFT_OAUTH2_CLIENT_SECRET}
      redirect-uri: ${MICROSOFT_OAUTH2_REDIRECT_URI}
      authorization-endpoint: https://login.microsoftonline.com/common/oauth2/v2.0/authorize
      token-endpoint: https://login.microsoftonline.com/common/oauth2/v2.0/token
      scopes: https://outlook.office365.com/IMAP.AccessAsUser.All offline_access

    google:  # Configuração futura
      client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
      client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
      redirect-uri: ${GOOGLE_OAUTH2_REDIRECT_URI}
      authorization-endpoint: https://accounts.google.com/o/oauth2/v2/auth
      token-endpoint: https://oauth2.googleapis.com/token
      scopes: https://mail.google.com/ openid email profile

    yahoo:  # Configuração futura
      client-id: ${YAHOO_OAUTH2_CLIENT_ID}
      client-secret: ${YAHOO_OAUTH2_CLIENT_SECRET}
      redirect-uri: ${YAHOO_OAUTH2_REDIRECT_URI}
      authorization-endpoint: https://api.login.yahoo.com/oauth2/request_auth
      token-endpoint: https://api.login.yahoo.com/oauth2/get_token
      scopes: mail-r mail-w
```

## 📝 Tarefas Específicas

### Tarefa 1: Criar Estrutura Base
- [ ] Criar interface `OAuth2Strategy`
- [ ] Criar `OAuth2StrategyFactory`
- [ ] Criar `OAuth2Service` (facade)
- [ ] Criar DTO genérico `OAuth2TokenResponse` (não específico da Microsoft)

### Tarefa 2: Refatorar Microsoft para Strategy
- [ ] Renomear `MicrosoftOAuth2ServiceImpl` para `MicrosoftOAuth2Strategy`
- [ ] Implementar interface `OAuth2Strategy` em `MicrosoftOAuth2Strategy`
- [ ] Implementar método `supportsEmailDomain()` para detectar domínios Microsoft
- [ ] Mover configurações Microsoft para estrutura multi-provider

### Tarefa 3: Atualizar Controllers
- [ ] Refatorar `MicrosoftOAuth2Controller` para `OAuth2Controller` genérico
- [ ] Adicionar `{provider}` como path variable
- [ ] Atualizar rotas para: `/api/v1/oauth2/{provider}/authorize/{id}`
- [ ] Atualizar rotas para: `/api/v1/oauth2/{provider}/callback`

### Tarefa 4: Atualizar Banco de Dados
- [ ] Adicionar coluna `oauth2_provider` em `EmailSearchConfig`
- [ ] Criar migração Liquibase para novo campo
- [ ] Atualizar `EmailSearchConfig` entity com novo campo

### Tarefa 5: Atualizar Integrações
- [ ] Modificar `EmailServiceImpl` para usar `OAuth2Service` (facade)
- [ ] Remover dependência direta de `MicrosoftOAuth2Service`
- [ ] Atualizar lógica de detecção de provedor

### Tarefa 6: Configuração
- [ ] Refatorar `application.yml` para estrutura multi-provider
- [ ] Criar `@ConfigurationProperties` para carregar configs de múltiplos provedores
- [ ] Criar classe `OAuth2ProviderConfig` para encapsular configs

### Tarefa 7: Testes e Documentação
- [ ] Atualizar `OAUTH2_SETUP.md` com novo padrão
- [ ] Adicionar seção sobre como adicionar novos provedores
- [ ] Criar exemplo de implementação de novo provedor

## 🔧 Requisitos Técnicos

1. **Backward Compatibility**: Manter compatibilidade com dados existentes (Microsoft)
2. **Spring Boot Best Practices**: Usar @Autowired, @Service, etc.
3. **Clean Code**: Seguir princípios SOLID
4. **Java 21**: Aproveitar features modernas do Java (records, pattern matching, etc.)
5. **Configuração Externa**: Tudo configurável via environment variables

## 🎨 Exemplo de Uso (Após Refatoração)

### Para Microsoft (mantém funcionamento atual):
```java
// Iniciar autorização
GET /api/v1/oauth2/microsoft/authorize/123

// Callback
GET /api/v1/oauth2/microsoft/callback?code=...&state=...
```

### Para Google (futuro):
```java
// Iniciar autorização
GET /api/v1/oauth2/google/authorize/456

// Callback
GET /api/v1/oauth2/google/callback?code=...&state=...
```

### No código (EmailServiceImpl):
```java
// ANTES (Microsoft-specific):
password = microsoftOAuth2Service.getValidAccessToken(emailSearchConfig);

// DEPOIS (Provider-agnostic):
password = oauth2Service.getValidAccessToken(emailSearchConfig);
// Factory detecta automaticamente o provedor baseado no email
```

## 🚀 Benefícios Esperados

1. **Extensibilidade**: Adicionar Google OAuth2 será apenas:
   - Criar `GoogleOAuth2Strategy implements OAuth2Strategy`
   - Adicionar configurações em `application.yml`
   - Spring Boot auto-registra via `@Service`

2. **Manutenibilidade**: Cada provedor isolado, mudanças não afetam outros

3. **Testabilidade**: Testar strategies independentemente

4. **Flexibilidade**: Usuários podem ter contas de diferentes provedores

## ⚠️ Considerações Importantes

1. **Migração de Dados**: Contas Microsoft existentes devem ter `oauth2_provider = 'microsoft'` preenchido automaticamente

2. **Fallback**: Sistema deve manter suporte para IMAP tradicional (sem OAuth2)

3. **Detecção Inteligente**: Factory deve detectar provedor por:
   - Campo `oauth2_provider` no banco (se já autorizado)
   - Domínio do email (se primeira autorização)

4. **Configuração Condicional**: Provedores sem configuração devem ser ignorados (não quebrar app)

## 📚 Referências

- Design Patterns: Strategy Pattern
- Spring Boot: Dependency Injection e @Autowired com List
- OAuth 2.0: Authorization Code Flow (RFC 6749)

## ✅ Critérios de Aceitação

- [ ] Implementação Microsoft funciona exatamente como antes
- [ ] Estrutura permite adicionar novos provedores facilmente
- [ ] Testes existentes continuam passando
- [ ] Documentação atualizada
- [ ] Migração Liquibase criada para novo campo
- [ ] Código segue padrões do projeto (Lombok, RequiredArgsConstructor, etc.)

## 🎯 Resultado Final

Ao final da refatoração, o projeto terá:
- ✅ Arquitetura extensível para múltiplos provedores OAuth2
- ✅ Código limpo e manutenível
- ✅ Fácil adicionar Google, Yahoo ou qualquer outro provedor
- ✅ Backward compatibility mantida
- ✅ Documentação atualizada

---

**Nota**: Esta refatoração é preparatória. Não é necessário implementar Google ou Yahoo agora, apenas criar a estrutura que permitirá adicioná-los facilmente no futuro.
