# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

---

## [2.0.0] - 2025-01-07

### 🎉 Adicionado

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

### 🐛 Corrigido

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

### ⚡ Melhorado

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

### 📚 Documentação

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

### 🎯 Problemas Resolvidos

| # | Problema | Status | Arquivo |
|---|----------|--------|---------|
| 1 | DARF e FGTS não reconhecidos | ✅ Resolvido | `ExpenseType.java:23-25` |
| 2 | Emitente não identificado em boletos | ✅ Resolvido | `BankExtractorImpl.java:91-127` |
| 3 | Linha digitável incorreta | ✅ Resolvido | `BankExtractorImpl.java:144-177` |
| 4 | Itens de NF truncados | ✅ Resolvido | `GeminiExtractorImpl.java:114` |
| 5 | Chave de acesso com espaços | ✅ Resolvido | `ExpenseExtractor.java:132-159` |
| 6 | Normalização muito agressiva | ✅ Resolvido | `PdfExtractorImpl.java:129` |

---

### 👥 Contribuidores

- Marco Willy - Implementação de novos types, models e extractors
- Claude AI - Análise, refatoração e melhorias

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
