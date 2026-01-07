# OAuth2 Provider Naming Convention

## 📋 Contexto

O projeto MailReader possui duas nomenclaturas para identificar provedores de email:

1. **`tb_email_config.provider`** - Identificadores de serviços de email (IMAP/SMTP)
2. **`oauth2_provider`** - Identificadores de plataformas OAuth2

## 🎯 Decisão: Manter Nomenclaturas Separadas

**Status:** ✅ APROVADO
**Data:** 2026-01-07
**Responsável:** MailReader Development Team

### Nomenclaturas Utilizadas

| Email Provider<br>(tb_email_config.provider) | OAuth2 Provider<br>(oauth2_provider) | Motivo |
|----------------------------------------------|--------------------------------------|---------|
| `outlook` | `microsoft` | Plataforma Microsoft Identity abrange Outlook, Hotmail, Live, Microsoft 365 |
| `gmail` | `google` | Plataforma Google OAuth2 abrange Gmail e Google Workspace |
| `yahoo` | `yahoo` | Mesmo nome em ambos contextos |
| `zoho` | `zoho` | Mesmo nome em ambos contextos |
| `icloud` | `apple` | Plataforma Apple OAuth2 |
| `aol` | `aol` | Mesmo nome em ambos contextos |

### Por que não unificar?

#### ✅ Vantagens de Manter Separado

1. **Semântica Correta:**
   - `tb_email_config.provider = 'outlook'` → Refere-se ao serviço de email Outlook
   - `oauth2_provider = 'microsoft'` → Refere-se à plataforma Microsoft Identity Platform

2. **Evita Breaking Changes:**
   - Código existente que busca `provider = 'outlook'` continua funcionando
   - Frontend/UI que mostra "Outlook" não precisa mudar
   - Queries SQL existentes não quebram

3. **Clareza Técnica:**
   - Microsoft Identity Platform ≠ Outlook (abrange mais serviços)
   - `imap.outlook.com` é específico do Outlook
   - Azure AD/Microsoft 365 são mais abrangentes

4. **Flexibilidade Futura:**
   - Podemos ter múltiplos provedores OAuth2 para um mesmo serviço
   - Ex: Microsoft pode ter OAuth2 corporativo e pessoal separados

#### ❌ Desvantagens de Unificar

1. **Breaking Changes:**
   - Todo código que referencia 'outlook' precisaria mudar
   - Migrations complexas no banco
   - Risco de quebrar funcionalidades existentes

2. **Perda de Semântica:**
   - 'microsoft' não é tão intuitivo quanto 'outlook' para usuários finais
   - Pode confundir desenvolvedores que mantêm IMAP/SMTP

3. **Impacto em Frontend:**
   - UIs que mostram "Outlook" teriam que ser atualizadas
   - Possível confusão para usuários

## 🔧 Implementação

### Mapeamento Entre Nomenclaturas

Criamos `OAuth2ProviderMapper` para converter entre os dois sistemas:

```java
// Email Provider → OAuth2 Provider
String oauth2 = OAuth2ProviderMapper.toOAuth2Provider("outlook");
// → "microsoft"

// OAuth2 Provider → Email Provider
String email = OAuth2ProviderMapper.toEmailProvider("microsoft");
// → "outlook"
```

### Migração Liquibase

A migração `20260105130000_add_oauth2_provider_column.xml` popula:

```sql
UPDATE tb_email_search_config
SET oauth2_provider = 'microsoft'
WHERE oauth2_enabled = true
  AND oauth2_access_token IS NOT NULL
  AND oauth2_provider IS NULL;
```

**Justificativa:** Registros existentes com OAuth2 eram todos Microsoft (implementação anterior).

### Estrutura de Dados

#### tb_email_config (Configurações de Servidor)

```
id | provider | imap_host           | imap_port | smtp_host           | smtp_port
---|----------|---------------------|-----------|---------------------|----------
1  | gmail    | imap.gmail.com      | 993       | smtp.gmail.com      | 587
2  | outlook  | imap.outlook.com    | 993       | smtp.office365.com  | 587
3  | yahoo    | imap.mail.yahoo.com | 993       | smtp.mail.yahoo.com | 587
```

**Uso:** Quando usuário escolhe provedor, sistema busca configurações IMAP/SMTP.

#### tb_email_search_config (Configurações Específicas)

```
id | email              | oauth2_enabled | oauth2_provider | oauth2_access_token
---|--------------------|-----------------|-----------------|-----------------------
1  | user@outlook.com   | true            | microsoft       | eyJ0eXAiOiJKV1QiLC...
2  | user@gmail.com     | true            | google          | ya29.a0AfH6SMBx7...
3  | user@example.com   | true            | microsoft       | eyJ0eXAiOiJKV1QiLC...
```

**Uso:** Sistema detecta `oauth2_provider` e chama strategy correspondente.

## 🔄 Fluxo de Autenticação

### 1. Usuário Seleciona Provedor

```java
// Frontend envia: "outlook"
String emailProvider = request.getParameter("provider"); // "outlook"

// Backend busca configurações IMAP/SMTP
EmailConfig config = emailConfigRepository.findByProvider(emailProvider);
// → imap.outlook.com, smtp.office365.com
```

### 2. Sistema Inicia OAuth2

```java
// Converte para OAuth2 provider
String oauth2Provider = OAuth2ProviderMapper.toOAuth2Provider(emailProvider);
// "outlook" → "microsoft"

// Gera URL de autorização
String authUrl = oauth2Service.generateAuthorizationUrl(configId, oauth2Provider);
// → /api/v1/oauth2/microsoft/authorize/123
```

### 3. Após Autorização

```java
// Sistema salva com oauth2_provider correto
emailSearchConfig.setOauth2Provider("microsoft");
emailSearchConfig.setOauth2Enabled(true);
emailSearchConfig.setOauth2AccessToken(token);
```

### 4. Uso Posterior

```java
// Sistema busca configuração
EmailSearchConfig config = repository.findById(id);

// Detecta provedor automaticamente
String oauth2Provider = config.getOauth2Provider(); // "microsoft"

// Usa strategy correspondente
String token = oauth2Service.getValidAccessToken(config);
// → OAuth2StrategyFactory seleciona MicrosoftOAuth2Strategy
```

## 📚 Referências

- **tb_email_config:** Tabela de referência com configurações IMAP/SMTP
- **oauth2_provider:** Campo em tb_email_search_config para identificar OAuth2 Strategy
- **OAuth2ProviderMapper:** Utilitário para conversão entre nomenclaturas
- **OAuth2Strategy:** Interface do Strategy Pattern para OAuth2

## 🔮 Futuro

Se houver necessidade de unificar no futuro:

1. **Opção A:** Adicionar coluna `oauth2_provider_name` em `tb_email_config`
2. **Opção B:** Criar tabela de mapeamento `tb_provider_oauth2_mapping`
3. **Opção C:** Migrar todos para nomenclatura unificada (alto risco)

**Recomendação atual:** Manter separado. A complexidade adicional é mínima e a clareza semântica compensa.

---

**Última atualização:** 2026-01-07
**Versão do documento:** 1.0
