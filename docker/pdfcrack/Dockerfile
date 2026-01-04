FROM debian:latest

# Instalar pdfcrack e qpdf
RUN apt-get update && \
    apt-get install -y pdfcrack qpdf && \
    apt-get clean

# Definir diretório de trabalho
WORKDIR /pdfs

# Manter o container em execução
CMD ["sh", "-c", "while :; do sleep 1; done"]
