#Passo 1: Construir a Imagem Docker
docker build -t pdfcrack-container .

#Passo 2: Criar uma Rede Docker (Opcional)
docker network create pdf-net

#Passo 3: Subir o Container do pdfcrack
  docker run -d --name pdfcrack-container \
    -v "C:/PDF/baixados:/pdfs" pdfcrack-container

#Passo 4: Verificar se o Container Está Rodando
docker ps

#Passo 5: Testar o pdfcrack no Container
docker exec pdfcrack \
  pdfcrack --file=/pdfs/arquivo-protegido.pdf --charset=0123456789 --minpw=1 --maxpw=6