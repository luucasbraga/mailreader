FROM debian:latest

# Instalar Tesseract OCR e pacotes necessários
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-por poppler-utils && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Definir o diretório de trabalho
WORKDIR /pdfs

# Manter o container em execução
CMD ["sh", "-c", "while :; do sleep 1; done"]
