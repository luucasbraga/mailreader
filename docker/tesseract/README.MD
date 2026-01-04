    # Passo 1: Construir a Imagem Docker
    docker build -t tesseract-container .

    # Passo 2: Criar uma Rede Docker (Opcional)
    docker network create pdf-net

    # Passo 3: Subir o Container do Tesseract
   docker run -d --name tesseract-container -v "C:/PDF:/pdfs" tesseract-container

    # Passo 4: Verificar se o Container Está Rodando
    docker ps

    # Passo 5: Testar o OCR no Container
    docker exec tesseract-container tesseract /pdfs/arquivo-imagem.png /pdfs/saida -l por
