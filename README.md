# Mailreader

**Mailreader** é uma aplicação Java Spring Boot que realiza a extração e o processamento de dados financeiros a partir de documentos PDF recebidos por e-mail. A aplicação integra-se com o **AWS S3** para armazenamento de arquivos, utiliza **Docker** para gerenciar containers e processa e-mails e anexos de documentos fiscais em diferentes formatos.

## Funcionalidades

- **Leitura de E-mails**: Conexão com servidores de e-mail para buscar e-mails com anexos, como PDFs, e extrair dados financeiros dos documentos.
- **Processamento de PDFs**: Suporta a descriptografia de PDFs protegidos por senha, extração de dados financeiros e categorização dos documentos, como NF-e, NFS-e, NF3-e, boletos, faturas, entre outros.
- **Armazenamento em S3**: Envia documentos processados para o **Amazon S3**, garantindo segurança e alta disponibilidade.
- **Utilização de Docker**: Containeriza o processo de quebra de senha de PDFs usando **Docker** para facilitar a execução em diferentes ambientes.
- **Agendamento de Jobs**: Utiliza **Quartz Scheduler** para agendar e executar tarefas como o processamento de e-mails e a limpeza de arquivos locais.

## Tecnologias Utilizadas

- **Java 21**: Linguagem principal utilizada para o desenvolvimento da aplicação.
- **Spring Boot**: Framework para construção e gerenciamento da aplicação.
- **AWS S3**: Armazenamento de arquivos na nuvem.
- **Docker**: Utilizado para isolar e gerenciar containers, especialmente no processo de quebra de senha de PDFs e leituras OCR.
- **PDFBox e iText**: Bibliotecas para manipulação de PDFs, como extração de dados e remoção de senha.
- **Quartz Scheduler**: Agendamento de tarefas, como o processamento de documentos e a execução de jobs.
- **JPA/Hibernate**: Para persistência de dados no banco de dados relacional.

## Instalação

### Pré-requisitos

Antes de começar, você precisará dos seguintes softwares instalados:

- **Java 21** ou superior.
- **Docker** para executar o container de quebra de senha de PDFs e o Tesseract (caso seja necessário).
- **AWS CLI** configurado para acesso ao S3.
- **Maven** ou **Gradle** para gerenciamento de dependências.

### Passos para a instalação

1. **Clone o repositório**:

   ```bash
   git clone http://group-git.group.local/grouppay/mailreader.git
   cd mailreader
