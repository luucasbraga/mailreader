# Configuração OAuth2 da Microsoft

Este documento descreve como configurar o OAuth 2.0 da Microsoft para permitir que o MailReader acesse emails de contas comerciais (Microsoft 365 / Exchange Online) e contas pessoais (@outlook.com, @hotmail.com, @live.com).

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Pré-requisitos](#pré-requisitos)
- [Configuração no Azure AD](#configuração-no-azure-ad)
- [Configuração da Aplicação](#configuração-da-aplicação)
- [Fluxo de Autorização](#fluxo-de-autorização)
- [Testando a Integração](#testando-a-integração)
- [Troubleshooting](#troubleshooting)

## 🎯 Visão Geral

A implementação usa **Authorization Code Flow** (Delegated Permissions), permitindo que:

- ✅ Cada cliente autorize o acesso à sua própria conta
- ✅ Funcione com contas pessoais Microsoft (@outlook.com, @hotmail.com, @live.com)
- ✅ Funcione com contas comerciais de qualquer tenant Microsoft 365
- ✅ Tokens sejam renovados automaticamente usando refresh tokens
- ✅ Autenticação seja transparente após autorização inicial

## 🔧 Pré-requisitos

1. **Conta Azure** com permissões para criar App Registrations
2. **Domínio público** para receber callbacks OAuth2 (ou usar localhost para testes)

## 🚀 Configuração no Azure AD

### Passo 1: Criar App Registration

1. Acesse o [Portal Azure](https://portal.azure.com)
2. Navegue para **Azure Active Directory** → **App registrations**
3. Clique em **New registration**
4. Preencha:
   - **Name**: `MailReader OAuth2`
   - **Supported account types**: Selecione **Accounts in any organizational directory and personal Microsoft accounts**
   - **Redirect URI**:
     - Type: `Web`
     - URI: `https://SEU_DOMINIO/api/v1/oauth2/microsoft/callback`
     - Para testes locais: `http://localhost:8080/api/v1/oauth2/microsoft/callback`

### Passo 2: Configurar Permissões (API Permissions)

1. No App Registration criado, vá em **API permissions**
2. Clique em **Add a permission**
3. Selecione **Microsoft Graph**
4. Selecione **Delegated permissions**
5. Adicione as seguintes permissões:
   - `IMAP.AccessAsUser.All` - Acesso IMAP em nome do usuário
   - `offline_access` - Permite obter refresh tokens
   - `openid` - Autenticação OpenID Connect
   - `profile` - Informações básicas do perfil
   - `email` - Acesso ao email do usuário

6. **Importante**: Como são permissões delegadas, **NÃO** é necessário "Grant admin consent"

### Passo 3: Criar Client Secret

1. No App Registration, vá em **Certificates & secrets**
2. Clique em **New client secret**
3. Adicione uma descrição (ex: "MailReader Production")
4. Escolha a validade (recomendado: 24 meses)
5. Clique em **Add**
6. **IMPORTANTE**: Copie o **Value** do secret imediatamente (não será mostrado novamente)

### Passo 4: Copiar Client ID

1. No App Registration, vá em **Overview**
2. Copie o **Application (client) ID**

## ⚙️ Configuração da Aplicação

### Variáveis de Ambiente

Configure as seguintes variáveis de ambiente:

```bash
# Client ID do App Registration
MICROSOFT_OAUTH2_CLIENT_ID=sua-client-id-aqui

# Client Secret gerado no passo anterior
MICROSOFT_OAUTH2_CLIENT_SECRET=seu-client-secret-aqui

# URL de callback (deve ser a mesma configurada no Azure)
MICROSOFT_OAUTH2_REDIRECT_URI=https://SEU_DOMINIO/api/v1/oauth2/microsoft/callback
```

### Exemplo de configuração local (.env)

```bash
MICROSOFT_OAUTH2_CLIENT_ID=26bb43d4-62eb-4014-9eea-b34f48542b55
MICROSOFT_OAUTH2_CLIENT_SECRET=abc123xyz789~.qwerty
MICROSOFT_OAUTH2_REDIRECT_URI=http://localhost:8080/api/v1/oauth2/microsoft/callback
```

## 🔐 Fluxo de Autorização

### 1. Iniciar Autorização

Para autorizar uma conta de email, acesse:

```
GET /api/v1/oauth2/microsoft/authorize/{emailSearchConfigId}
```

**Exemplo**:
```
https://seu-dominio.com/api/v1/oauth2/microsoft/authorize/123
```

### 2. Usuário Concede Permissões

O usuário será redirecionado para a página de login da Microsoft, onde deverá:
- Fazer login com sua conta Microsoft
- Conceder permissões para o MailReader acessar seus emails

### 3. Callback e Armazenamento de Tokens

Após autorização, a Microsoft redireciona para:
```
/api/v1/oauth2/microsoft/callback?code=...&state=...
```

O sistema automaticamente:
- Troca o authorization code por access token e refresh token
- Armazena os tokens no banco de dados
- Habilita OAuth2 para o EmailSearchConfig
- Redireciona para página de sucesso

### 4. Renovação Automática de Tokens

O sistema renova automaticamente os tokens quando:
- O access token está expirado
- Faltam menos de 5 minutos para expiração

A renovação usa o refresh token armazenado e é **totalmente transparente**.

## 🧪 Testando a Integração

### Teste 1: Verificar Configuração

```bash
# Verificar se as variáveis estão configuradas
curl http://localhost:8080/actuator/env | grep MICROSOFT_OAUTH2
```

### Teste 2: Iniciar Fluxo OAuth2

1. Acesse no navegador:
   ```
   http://localhost:8080/api/v1/oauth2/microsoft/authorize/1
   ```
   (Substitua `1` pelo ID da configuração de email)

2. Faça login com uma conta Microsoft
3. Conceda as permissões
4. Verifique se foi redirecionado para a página de sucesso

### Teste 3: Verificar Tokens no Banco

```sql
SELECT
    id,
    email,
    oauth2_enabled,
    oauth2_token_expiry,
    LENGTH(oauth2_access_token) as token_length
FROM tb_email_search_config
WHERE id = 1;
```

### Teste 4: Processar Emails

Execute o job de processamento de emails. O sistema deve:
- Detectar que OAuth2 está habilitado
- Usar o access token armazenado
- Renovar automaticamente se necessário

## 🔍 Troubleshooting

### Erro: "AADSTS700016: Application not found in the directory"

**Causa**: Client ID incorreto ou App Registration não existe

**Solução**:
- Verifique se copiou o Client ID corretamente
- Confirme que o App Registration existe no Azure AD

### Erro: "AADSTS50011: The redirect URI specified does not match"

**Causa**: Redirect URI configurado no Azure não corresponde ao enviado

**Solução**:
- Verifique se a variável `MICROSOFT_OAUTH2_REDIRECT_URI` está correta
- Confirme que a URI está cadastrada no Azure AD (Web platform)
- Atenção: http vs https, localhost vs domínio

### Erro: "invalid_client - AADSTS7000215"

**Causa**: Client secret incorreto ou expirado

**Solução**:
- Gere um novo client secret no Azure
- Atualize a variável `MICROSOFT_OAUTH2_CLIENT_SECRET`

### Erro: "Consent required"

**Causa**: Usuário não concedeu todas as permissões necessárias

**Solução**:
- Inicie o fluxo OAuth2 novamente
- Certifique-se de que o usuário concedeu todas as permissões solicitadas

### Erro: "Falha ao obter token OAuth2 delegado"

**Causa**: Refresh token pode estar expirado ou inválido

**Solução**:
- Execute novamente o fluxo de autorização para o EmailSearchConfig
- Verifique se o campo `oauth2_refresh_token` está preenchido no banco

### Token expirando rapidamente

**Causa**: Access tokens da Microsoft expiram em 1 hora por padrão

**Solução**:
- Isso é esperado! O sistema renova automaticamente usando refresh token
- Refresh tokens são válidos por até 90 dias (ou até serem usados)

## 📊 Estrutura do Banco de Dados

Novos campos adicionados em `tb_email_search_config`:

```sql
oauth2_enabled         BOOLEAN      -- Se OAuth2 está habilitado
oauth2_access_token    TEXT         -- Access token atual
oauth2_refresh_token   TEXT         -- Refresh token para renovação
oauth2_token_expiry    TIMESTAMP    -- Data/hora de expiração do access token
```

## 🔄 Migração de Contas Existentes

### Contas com Client Credentials (antigo)

O sistema mantém compatibilidade com o método antigo:
- Se `oauth2_enabled = false`, usa client credentials (se for conta Microsoft)
- Se `oauth2_enabled = true`, usa Authorization Code Flow (delegado)

### Como migrar para o novo método

1. Para cada EmailSearchConfig que usa conta Microsoft:
2. Inicie o fluxo OAuth2:
   ```
   GET /api/v1/oauth2/microsoft/authorize/{emailSearchConfigId}
   ```
3. Usuário concede permissões
4. Sistema atualiza automaticamente `oauth2_enabled = true`

## 📝 Notas Importantes

1. **Refresh tokens expiram** após 90 dias de inatividade. Se uma conta ficar 90 dias sem ser acessada, será necessário reautorizar.

2. **Revogação de acesso**: Se o usuário revogar as permissões na conta Microsoft, será necessário reautorizar.

3. **Multi-tenant**: Esta configuração permite acessar contas de qualquer tenant Azure AD, não apenas do tenant específico.

4. **Contas pessoais**: Funciona perfeitamente com @outlook.com, @hotmail.com, @live.com.

5. **Segurança**: Client secret deve ser protegido como senha. Nunca commitar em repositórios.

## 🆘 Suporte

Para problemas ou dúvidas:
1. Verifique os logs da aplicação
2. Consulte a documentação oficial da Microsoft: https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow
3. Entre em contato com a equipe de desenvolvimento
