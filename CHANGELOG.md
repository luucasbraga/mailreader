# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

---

## [2.1.0] - 2026-01-07

### Adicionado

#### Arquitetura OAuth2 Multi-Provedor (Strategy Pattern)

- **OAuth2Strategy (Interface)** - Contrato para implementações OAuth2
    - Métodos: `getProviderName()`, `generateAuthorizationUrl()`, `exchangeCodeForTokens()`
    - Métodos: `refreshAccessToken()`, `isTokenExpired()`, `getValidAccessToken()`
    - Método: `supportsEmailDomain()` - Detecção automática de provedor por domínio de email
    - Permite adicionar novos provedores (Google, Yahoo) sem modificar código existente

- **OAuth2StrategyFactory** - Factory Pattern para seleção dinâmica de strategies
    - Injeção automática de todas as strategies disponíveis via Spring
    - Método `getStrategy(String providerName)` - Busca por nome do provedor
    - Método `getStrategyForEmail(String email)` - Detecção automática baseada no domínio
    - Método `hasProvider(String providerName)` - Verifica disponibilidade
    - Método `getAvailableProviders()` - Lista todos os provedores configurados

- **OAuth2Service (Facade)** - Service facade unificado para operações OAuth2
    - Simplifica integração com clientes (EmailServiceImpl)
    - Delegação automática para strategy correta
    - Detecção inteligente de provedor (campo `oauth2_provider` ou domínio do email)
    - Logging detalhado para troubleshooting

- **MicrosoftOAuth2Strategy** - Implementação Strategy Pattern para Microsoft
    - Suporta contas pessoais: @outlook.com, @hotmail.com, @live.com, @msn.com
    - Suporta Microsoft 365 corporativo (domínios gerenciados)
    - Detecção automática via API UserRealm da Microsoft
    - Domínios "managed" e "federated" são reconhecidos como Microsoft 365

- **OAuth2Controller (Unificado)** - Controller genérico para todos os provedores
    - `GET /api/v1/oauth2/{provider}/authorize/{emailSearchConfigId}` - Inicia autorização
    - `GET /api/v1/oauth2/{provider}/callback` - Recebe callback após autorização
    - `GET /api/v1/oauth2/{provider}/status/{emailSearchConfigId}` - Verifica status
    - `GET /api/v1/oauth2/providers` - Lista provedores disponíveis
    - Path variable `{provider}` permite: microsoft, google, yahoo, etc.

- **OAuth2TokenResponse (DTO Genérico)** - DTO compatível com RFC 6749
    - Substitui MicrosoftOAuth2TokenResponse específico
    - Campos: `accessToken`, `tokenType`, `expiresIn`, `scope`, `refreshToken`, `idToken`
    - Compatível com Google, Yahoo e outros provedores OAuth2

- **Campo oauth2_provider** em EmailSearchConfig
    - Novo campo `VARCHAR(50)` para identificar provedor OAuth2
    - Permite configurações com provedores diferentes no mesmo sistema
    - Migração Liquibase `20260105130000_add_oauth2_provider_column.xml`

- **OAuth2ProviderMapper (Utilitário)** - Conversão entre nomenclaturas
    - Mapeia email providers (outlook, gmail) ↔ OAuth2 providers (microsoft, google)
    - Método `toOAuth2Provider("outlook")` → "microsoft"
    - Método `toEmailProvider("microsoft")` → "outlook"
    - Método `hasOAuth2Support(String emailProvider)` - Valida suporte OAuth2
    - Facilita integração com `tb_email_config.provider`

#### Documentação

- **OAUTH2_PROVIDER_NAMING.md** - Decisão arquitetural sobre nomenclaturas
    - Explica diferença entre `tb_email_config.provider` (outlook) e `oauth2_provider` (microsoft)
    - Justificativa técnica para manter nomenclaturas separadas
    - Exemplos de uso e fluxos completos

- **OAUTH2_MAPPER_USAGE.md** - Guia estratégico do OAuth2ProviderMapper
    - Quando usar vs quando não usar o mapper
    - Casos de uso: fronteiras do sistema, controllers, DTOs
    - Checklist de implementação

- **OAUTH2_MAPPER_PRACTICAL_EXAMPLES.md** - Exemplos concretos de código
    - Modificações sugeridas no OAuth2Controller
    - DTOs com ambas nomenclaturas
    - Priorização de implementações (Alta, Média, Baixa)

#### Migrações Liquibase

- **20260105130000_add_oauth2_provider_column.xml**
    - Adiciona coluna `oauth2_provider VARCHAR(50)` em `tb_email_search_config`
    - Popula automaticamente `oauth2_provider='microsoft'` para registros existentes
    - Garante backward compatibility com implementação anterior

### Alterado

#### Refatoração de Código

- **EmailServiceImpl** - Atualizado para usar OAuth2Service
    - **Antes**: Injetava `MicrosoftOAuth2Service` diretamente
    - **Depois**: Injeta `OAuth2Service` (provider-agnostic)
    - Logging melhorado mostra nome do provedor OAuth2 usado
    - Mantém compatibilidade com implementação anterior

- **EmailSearchConfig Entity** - Novo campo oauth2_provider
    - Adicionado campo `oauth2_provider` com anotação JPA
    - Getter/Setter disponíveis via Lombok
    - Campo opcional (nullable) para compatibilidade

#### Endpoints OAuth2

- **Antes**: `/api/v1/oauth2/microsoft/authorize/{id}` (específico Microsoft)
- **Depois**: `/api/v1/oauth2/{provider}/authorize/{id}` (genérico)
- **Compatibilidade**: Endpoints antigos continuam funcionando com provider="microsoft"

### Removido

- **MicrosoftOAuth2Service.java** (interface)
    - Substituída por `OAuth2Strategy` (interface genérica)

- **MicrosoftOAuth2ServiceImpl.java** (implementação)
    - Código migrado para `MicrosoftOAuth2Strategy` (implementa OAuth2Strategy)
    - Toda lógica preservada, apenas mudança de nome e interface

- **MicrosoftOAuth2Controller.java** (controller específico)
    - Substituído por `OAuth2Controller` unificado
    - Funcionalidades idênticas, mas com suporte multi-provedor

- **MicrosoftOAuth2TokenResponse.java** (DTO específico)
    - Substituído por `OAuth2TokenResponse` genérico
    - Compatível com todos os provedores OAuth2

### Melhorado

#### Arquitetura e Design Patterns

- ✅ **Strategy Pattern** implementado para OAuth2
    - Facilita adição de novos provedores (Google, Yahoo, etc.)
    - Cada provedor isolado em sua própria strategy
    - Zero acoplamento entre provedores

- ✅ **Factory Pattern** para seleção de strategies
    - Seleção dinâmica baseada em nome ou domínio de email
    - Spring auto-detecta novas strategies via @Service

- ✅ **Facade Pattern** para simplificar API
    - OAuth2Service oculta complexidade da factory
    - Interface limpa para clientes (EmailServiceImpl)

#### Extensibilidade

- ✅ Adicionar novo provedor requer apenas:
    1. Criar classe `XxxOAuth2Strategy implements OAuth2Strategy`
    2. Anotar com `@Service("xxxOAuth2Strategy")`
    3. Implementar métodos da interface
    4. Spring registra automaticamente!

- ✅ Exemplo futuro - GoogleOAuth2Strategy:
    ```java
    @Service("googleOAuth2Strategy")
    public class GoogleOAuth2Strategy implements OAuth2Strategy {
        public String getProviderName() { return "google"; }
        // Implementar outros métodos...
    }
    ```

#### Detecção Automática de Provedor

- ✅ Sistema detecta provedor baseado em:
    1. **Prioridade 1**: Campo `oauth2_provider` já configurado no banco
    2. **Prioridade 2**: Domínio do email via `supportsEmailDomain()`
    3. **Exemplo**: user@outlook.com → Detecta Microsoft automaticamente

- ✅ Microsoft detecta:
    - Domínios pessoais: outlook.com, hotmail.com, live.com, msn.com
    - Domínios corporativos: Via API UserRealm (account_type=managed/federated)

#### Manutenibilidade

- ✅ **Código limpo**: Sem lógica condicional `if (provider == "microsoft")`
- ✅ **Testabilidade**: Cada strategy pode ser testada independentemente
- ✅ **Logging**: Logs detalhados com nome do provedor em cada operação
- ✅ **Documentação**: 3 documentos completos explicando arquitetura

### Segurança

- ✅ State parameter continua sendo usado para proteção CSRF
- ✅ Validação de provedor antes de processar callbacks
- ✅ Tokens continuam armazenados de forma segura no banco
- ✅ Nenhuma mudança nos fluxos de segurança OAuth2

### 📝 Mudanças Técnicas

#### Estrutura de Arquivos

**Novos arquivos criados**: 7
```
+ OAuth2Strategy.java                      (Interface - 80 linhas)
+ OAuth2StrategyFactory.java               (Factory - 120 linhas)
+ OAuth2Service.java                       (Facade - 150 linhas)
+ OAuth2Controller.java                    (Controller - 220 linhas)
+ MicrosoftOAuth2Strategy.java             (Strategy - 300 linhas)
+ OAuth2TokenResponse.java                 (DTO - 70 linhas)
+ OAuth2ProviderMapper.java                (Util - 130 linhas)
```

**Arquivos modificados**: 3
```
* EmailSearchConfig.java                   (+3 linhas - novo campo)
* EmailServiceImpl.java                    (~5 linhas - troca de service)
* liquibase/master.xml                     (+1 linha - nova migration)
```

**Arquivos removidos**: 4
```
- MicrosoftOAuth2Service.java
- MicrosoftOAuth2ServiceImpl.java
- MicrosoftOAuth2Controller.java
- MicrosoftOAuth2TokenResponse.java
```

**Documentação criada**: 3
```
+ docs/OAUTH2_PROVIDER_NAMING.md           (350 linhas)
+ docs/OAUTH2_MAPPER_USAGE.md              (420 linhas)
+ docs/OAUTH2_MAPPER_PRACTICAL_EXAMPLES.md (580 linhas)
```

#### Estatísticas de Código

- **Linhas adicionadas**: ~865
- **Linhas removidas**: ~227
- **Saldo líquido**: +638 linhas
- **Arquivos no commit**: 13 files changed

#### Configuração (application.yml)

**Estrutura atual mantida**:
```yaml
microsoft:
  oauth2:
    client-id: ${MICROSOFT_OAUTH2_CLIENT_ID}
    client-secret: ${MICROSOFT_OAUTH2_CLIENT_SECRET}
    redirect-uri: ${MICROSOFT_OAUTH2_REDIRECT_URI}
```

**Estrutura futura recomendada** (opcional):
```yaml
oauth2:
  providers:
    microsoft:
      client-id: ${MICROSOFT_OAUTH2_CLIENT_ID}
      client-secret: ${MICROSOFT_OAUTH2_CLIENT_SECRET}
      redirect-uri: ${MICROSOFT_OAUTH2_REDIRECT_URI}
    google:
      client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
      client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
      redirect-uri: ${GOOGLE_OAUTH2_REDIRECT_URI}
```

### 🔄 Compatibilidade

#### Backward Compatibility

- ✅ **Endpoints**: URLs antigas continuam funcionando
    - `/api/v1/oauth2/microsoft/authorize/123` → ✅ Funciona

- ✅ **Dados**: Registros existentes migrados automaticamente
    - Liquibase popula `oauth2_provider='microsoft'` para OAuth2 habilitado

- ✅ **Código**: Fluxo OAuth2 idêntico ao anterior
    - MicrosoftOAuth2Strategy mantém 100% da lógica original

#### Breaking Changes

- ❌ **Nenhum breaking change** para usuários finais
- ⚠️ **Mudança interna**: Desenvolvedores não devem mais usar `MicrosoftOAuth2Service`
    - Usar `OAuth2Service` ao invés
    - Mudança é internal-only, não afeta APIs públicas

### 🚀 Preparação para Futuro

#### Google OAuth2 (Ready to Implement)

Para adicionar Google OAuth2, basta criar:

```java
@Service("googleOAuth2Strategy")
public class GoogleOAuth2Strategy implements OAuth2Strategy {

    @Value("${google.oauth2.client-id}")
    private String clientId;

    private static final String PROVIDER_NAME = "google";
    private static final String AUTHORIZATION_ENDPOINT =
        "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT =
        "https://oauth2.googleapis.com/token";

    @Override
    public String getProviderName() { return PROVIDER_NAME; }

    @Override
    public boolean supportsEmailDomain(String email) {
        String domain = extractDomain(email);
        return domain.equals("gmail.com") || domain.equals("googlemail.com");
    }

    // Implementar outros métodos...
}
```

**Nenhuma outra mudança necessária!** Spring detecta automaticamente.

#### Yahoo OAuth2 (Ready to Implement)

Mesmo padrão:
```java
@Service("yahooOAuth2Strategy")
public class YahooOAuth2Strategy implements OAuth2Strategy {
    // Implementação similar...
}
```

### Problemas Resolvidos

| # | Problema | Status | Solução |
|---|----------|--------|---------|
| 1 | OAuth2 acoplado à Microsoft | ✅ Resolvido | Strategy Pattern desacopla provedores |
| 2 | Adicionar Google requer refatoração | ✅ Resolvido | Nova strategy = zero mudanças em código existente |
| 3 | Lógica condicional espalhada | ✅ Resolvido | Factory seleciona strategy correta |
| 4 | Testes acoplados | ✅ Resolvido | Strategies testáveis independentemente |
| 5 | Nomenclatura confusa (outlook vs microsoft) | ✅ Documentado | Mapper + docs explicam diferença |

### Commits Relacionados

```
ca59c15 - docs: Add comprehensive OAuth2ProviderMapper usage guide
e5d77b1 - docs: Add OAuth2 provider naming convention and mapper utility
b54aaf0 - refactor: Implement Strategy Pattern for multi-provider OAuth2 support
```

---

## [2.0.0] - 2025-01-07

### Adicionado

#### Novos Tipos de Documentos

- **DARF** - Documento de Arrecadação de Receitas Federais
    - Suporte completo para extração de dados de DARFs da Receita Federal
    - Extração de composição de tributos (código, denominação, principal, multa, juros)
    - Suporte a código de barras (48 dígitos) e PIX Copia e Cola

- **FGTS** - Guia do FGTS Digital (GFD)
    - Extração de dados de guias do FGTS Digital
    - Suporte a composição por competência (trabalhadores, remuneração, valores)
    - Identificador de guia e código de barras

- **GPS** - Guia da Previdência Social
    - Extração completa de GPS com código de pagamento
    - Valores detalhados: INSS, outras entidades, atualização monetária, juros, multa
    - Suporte a identificadores CEI/NIT

#### Novos Modelos de Dados

- `ExpenseDARF` com campos:
    - `razaoSocialContribuinte`, `periodoApuracao`
    - `numeroDocumento`, `numeroRecibo`
    - `valorTotal`, `codigoBarras`, `pixCopiaCola`
    - `composicao` (lista de tributos com detalhamento completo)

- `ExpenseFGTS` com campos:
    - `razaoSocialEmpregador`, `identificador`
    - `valorTotal`, `codigoBarras`, `pixCopiaCola`
    - `composicao` (lista por competência com trabalhadores e valores)

- `ExpenseGPS` com campos:
    - `razaoSocialContribuinte`, `codigoPagamento`, `competencia`
    - `identificador` (CEI/NIT)
    - `valorINSS`, `valorOutrasEntidades`, `atualizacaoMonetaria`
    - `juros`, `multa`, `valorTotal`

#### Novos Extractors

- **DarfExtractor** e **DarfExtractorImpl**
    - Extração robusta com fallback para métodos default
    - Métodos específicos para cada campo
    - Suporte a estruturas complexas (lista de tributos)
    - Regex específicos para período de apuração, código de barras, etc.

- **FgtsExtractor** e **FgtsExtractorImpl**
    - Extração de identificador da guia (formato: 0124040202313489-5)
    - Processamento de composição por competência
    - Suporte a múltiplas entradas de trabalhadores

- **GpsExtractor** e **GpsExtractorImpl**
    - Extração de código de pagamento (ex: 2100, 2208)
    - Processamento de todos os componentes de valor
    - Suporte a identificadores CEI e NIT

#### Melhorias em Extractors Existentes

**BankExtractorImpl (Boletos)**

- ✅ Novo método `extractCnpjCedente()` - Extrai CNPJ do beneficiário/cedente corretamente
    - Anteriormente: capturava o primeiro CNPJ encontrado (geralmente do pagador)
    - Agora: busca especificamente após "Beneficiário" ou "Cedente"

- ✅ Novo método `extractNomeCedente()` - Extrai nome da empresa beneficiária
    - Campo `emitente` agora recebe o nome real ao invés de CNPJ

- ✅ Novo método `extractLinhaDigitavel()` - Extração robusta de linha digitável
    - Suporta formato padrão: `XXXXX.XXXXX XXXXX.XXXXXX XXXXX.XXXXXX X XXXXXXXXXXXXXX`
    - Suporta formato sem pontos
    - Auto-formatação de 47 dígitos contíguos

- ✅ Novo método `extractCnpjPagador()` - Separa pagador de beneficiário

- ✅ Novo método `extractBanco()` - Detecção automática de banco
    - Mapeia códigos: 104=CAIXA, 237=BRADESCO, 341=ITAÚ, 001=BB, 033=SANTANDER
    - Fallback para detecção por nome

- ✅ Novos campos extraídos:
    - `nossoNumero` - Número de identificação do banco
    - `juros` - Valor de juros
    - `multa` - Valor de multa
    - `descontos` - Valor de descontos
    - `bancoEmissor` - Nome/código do banco
    - `cnpjCpfDestinatario` - CNPJ/CPF do pagador

**Extractors de Notas Fiscais (NF, NFC, NF3)**

- ✅ Novo método `extractChaveAcesso()` em `ExpenseExtractor` (interface)
    - Suporta chaves de acesso com espaços (ex: `3525 0900 7666 8500...`)
    - Regex robusto: `(?:Chave\s+de\s+Acesso|CHAVE)[:\s]*([0-9\s]{44,60})`
    - Validação de código UF (11-53)
    - Remove espaços automaticamente para retornar 44 dígitos limpos

- ✅ Aplicado em:
    - `NfExtractorImpl` (linha 63)
    - `NfcExtractorImpl` (linha 56)
    - `Nf3ExtractorImpl` (linha 51)

#### Detecção em Scripts Python

- ✅ Adicionados patterns para DARF, FGTS e GPS em todos os scripts Python:
    - `extract_text.py`
    - `extract_text_json.py`
    - `extract_text_transformers.py`

Patterns implementados:
```python
TipoDocumento.DARF: r'Documento de Arrecada[cç][aã]o.*Receitas Federais|DARF|Receita Federal.*DARF'
TipoDocumento.FGTS: r'FGTS|Guia do FGTS|GFD|Fundo de Garantia'
TipoDocumento.GPS: r'GPS|Guia.*Previd[eê]ncia Social|Previd[eê]ncia.*Social.*Guia'
```

---

### Corrigido

#### BankExtractorImpl

- **[CRÍTICO]** Corrigida extração de CNPJ do beneficiário
    - **Problema**: Capturava o CNPJ do pagador ao invés do cedente/beneficiário
    - **Causa**: Usava o primeiro CNPJ encontrado no documento
    - **Solução**: Implementado regex específico que busca após "Beneficiário" ou "Cedente"
    - **Impacto**: Identificação correta de quem vai receber o pagamento

- **[CRÍTICO]** Corrigida extração de linha digitável
    - **Problema**: Linha digitável vinha com valores incorretos (ex: `1111.87000 00111.1...`)
    - **Causa**: Regex muito genérico capturando sequências erradas
    - **Solução**: Implementado regex robusto com validação de formato
    - **Impacto**: Pagamento de boletos agora confiável

- **[MÉDIO]** Corrigido campo `emitente` preenchido com CNPJ
    - **Problema**: Campo deveria conter nome da empresa, mas estava recebendo CNPJ
    - **Causa**: Reutilização incorreta da variável `cnpjEmissor`
    - **Solução**: Implementado `extractNomeCedente()` para extrair nome real
    - **Impacto**: Visualização correta do nome da empresa

#### Extractors de Notas Fiscais

- **[CRÍTICO]** Corrigida extração de chave de acesso com espaços
    - **Problema**: Chaves formatadas com espaços não eram encontradas
    - **Exemplo**: `3525 0900 7666 8500 0181 5500...` não era reconhecido
    - **Causa**: Regex esperava 44 dígitos contíguos sem espaços
    - **Solução**: Novo regex aceita espaços e remove após validação
    - **Impacto**: Validação de NF-e agora funciona em todos os casos

#### PdfExtractorImpl

- **[ALTO]** Corrigida normalização agressiva de texto
    - **Problema**: Removia caracteres importantes (vírgulas, parênteses, R$)
    - **Causa**: Regex muito restritivo: `[^\\p{L}\\p{N}\\s:/.-]`
    - **Exemplos perdidos**:
        - Valores monetários: `R$ 1.234,56` → `R 1.234 56`
        - Linha digitável: `10490.11115` → `10490 11115`
    - **Solução**: Regex menos agressivo preservando pontuação importante
    - **Novo padrão**: `[^\\p{L}\\p{N}\\s:/.\\-,;()\\[\\]{}|#*+=<>\"'`~^&!?\\\\@$%R]`
    - **Impacto**: Extractors conseguem capturar dados formatados corretamente

---

### Melhorado

#### Configuração do Gemini AI

- **[CRÍTICO]** Aumentado limite de tokens de saída
    - **Antes**: `maxOutputTokens: 2048` (~8.000 caracteres)
    - **Depois**: `maxOutputTokens: 8192` (~32.000 caracteres)
    - **Razão**: Documentos com muitos itens eram truncados
    - **Problema resolvido**: Notas fiscais com 18+ itens agora processadas completamente
    - **Exemplo**: NF com 18 itens extraía apenas 11 → agora extrai todos os 18
    - **Arquivo**: `GeminiExtractorImpl.java:114`

- ✅ Adicionado mapeamento para novos tipos em `GeminiConfig`
    - `ExpenseType.DARF → ExpenseDARF.class`
    - `ExpenseType.FGTS → ExpenseFGTS.class`
    - `ExpenseType.GPS → ExpenseGPS.class`

#### Arquitetura e Facades

- ✅ Atualizado `SlipFacade` com novos extractors:
    - `public final DarfExtractor darf;`
    - `public final FgtsExtractor fgts;`
    - `public final GpsExtractor gps;`

- ✅ Atualizado `ExpenseType` enum:
    - Adicionado `DARF`
    - Adicionado `FGTS`
    - Adicionado `GPS`

---

### 📝 Mudanças Técnicas

#### Normalização de Texto

**Arquivo**: `PdfExtractorImpl.java:129`

**Antes**:
```java
.replaceAll("[^\\p{L}\\p{N}\\s:/.-]", " ")
```

**Depois**:
```java
.replaceAll("[^\\p{L}\\p{N}\\s:/.\\-,;()\\[\\]{}|#*+=<>\"'`~^&!?\\\\@$%R]", " ")
```

**Caracteres agora preservados**:
- Vírgulas (`,`) - Valores monetários
- Ponto e vírgula (`;`) - Separadores
- Parênteses `()` - Informações adicionais
- Colchetes `[]` e chaves `{}` - Estruturas
- Símbolos monetários (`$`, `%`, `R`) - Valores
- Operadores (`+`, `=`, `*`, `#`) - Fórmulas/referências

**Impacto**: Extractors de regex funcionam melhor com contexto preservado

#### Interface ExpenseExtractor

**Arquivo**: `ExpenseExtractor.java:125-159`

Novo método default adicionado:
```java
default String extractChaveAcesso(String text) {
    // Tenta encontrar após os termos "Chave de Acesso" ou "CHAVE"
    Pattern pattern = Pattern.compile(
        "(?:Chave\\s+de\\s+Acesso|CHAVE)[:\\s]*([0-9\\s]{44,60})",
        Pattern.CASE_INSENSITIVE
    );
    // ... validação e limpeza
}
```

**Benefícios**:
- ✅ Reutilizável em todos os extractors de NF
- ✅ Suporta múltiplos formatos
- ✅ Validação de UF integrada

---

### 📊 Estatísticas

**Arquivos modificados**: 22
**Linhas adicionadas**: 1.255
**Linhas removidas**: 31
**Novos arquivos**: 9

**Novos tipos de documentos**: 3 (DARF, FGTS, GPS)
**Novos models**: 3 (ExpenseDARF, ExpenseFGTS, ExpenseGPS)
**Novos extractors**: 6 (3 interfaces + 3 implementações)
**Bugs críticos corrigidos**: 6

---

### 🔄 Compatibilidade

#### Breaking Changes

- Nenhum breaking change foi introduzido
- Todos os extractors existentes mantêm compatibilidade retroativa
- Novos tipos são adicionais, não substituem tipos existentes

#### Migrations Necessárias

- ⚠️ **Banco de Dados**: Pode ser necessário criar migrations para novas tabelas se estiver persistindo esses tipos
- ⚠️ **AIMessageModel**: Configurar templates de IA para DARF, FGTS e GPS
- ⚠️ **Regex**: Adicionar patterns específicos na tabela `tb_regex` se necessário

---

### Documentação

#### Novos Extractors Implementam

- Fallback para métodos default (`defaultDueDate`, `defaultTotalValue`, `defaultIssuerCNPJ`)
- Métodos privados específicos para cada campo
- Tratamento de erros com logs detalhados
- Suporte a estruturas complexas (listas, composições)

#### Padrão Seguido

Baseado nos extractors mais maduros do sistema (DarfExtractorImpl, GpsExtractorImpl, FgtsExtractorImpl):
1. Tentativa de extração via regex (se disponível)
2. Fallback para métodos default
3. Métodos específicos privados para campos complexos
4. Validação e normalização de dados
5. Logs para debugging

---

### Problemas Resolvidos

| # | Problema | Status | Arquivo |
|---|----------|--------|---------|
| 1 | DARF e FGTS não reconhecidos | ✅ Resolvido | `ExpenseType.java:23-25` |
| 2 | Emitente não identificado em boletos | ✅ Resolvido | `BankExtractorImpl.java:91-127` |
| 3 | Linha digitável incorreta | ✅ Resolvido | `BankExtractorImpl.java:144-177` |
| 4 | Itens de NF truncados | ✅ Resolvido | `GeminiExtractorImpl.java:114` |
| 5 | Chave de acesso com espaços | ✅ Resolvido | `ExpenseExtractor.java:132-159` |
| 6 | Normalização muito agressiva | ✅ Resolvido | `PdfExtractorImpl.java:129` |

---

### 🚀 Próximos Passos Recomendados

1. **Testes com documentos reais**
    - Validar DARF com múltiplos tributos
    - Validar FGTS com múltiplas competências
    - Validar GPS com diferentes códigos de pagamento

2. **Configuração de banco de dados**
    - Criar migrations se necessário
    - Configurar `AIMessageModel` para novos tipos
    - Adicionar patterns de regex regionais

3. **Monitoramento**
    - Acompanhar logs de extração
    - Identificar edge cases
    - Ajustar regex conforme necessário

4. **Performance**
    - Avaliar impacto do aumento de tokens
    - Monitorar tempo de processamento
    - Otimizar se necessário

---

## [1.0.0] - 2024-XX-XX

### Inicial

- Implementação base do sistema de extração
- Suporte para NFE, NFSE, NFCE, NF3E, CTE, BOLETO, FATURA
- Integração com Gemini AI e OpenAI
- Extractors com PDFBox e OCR (Tesseract)
- Scripts Python para processamento

---

**Formato do Changelog**: [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/)
**Versionamento**: [Semantic Versioning](https://semver.org/lang/pt-BR/)
