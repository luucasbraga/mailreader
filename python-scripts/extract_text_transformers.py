import cv2
import json
import numpy as np
import os
import re
import sys
import unicodedata
from pdf2image import convert_from_path
from transformers import TrOCRProcessor, VisionEncoderDecoderModel


class TipoDocumento:
    NFE = "NF-e"
    NFSE = "NFS-e"
    NFCE = "NFC-e"
    NF3E = "NF3-e"
    CTE = "CT-e"
    BOLETO = "Boleto"
    FATURA = "Fatura"
    DARF = "DARF"
    FGTS = "FGTS"
    GPS = "GPS"
    DESCONHECIDO = "Desconhecido"


def identificar_tipo_documento(texto):
    patterns = {
        TipoDocumento.DARF: r'Documento de Arrecada[cç][aã]o.*Receitas Federais|DARF|Receita Federal.*DARF',
        TipoDocumento.FGTS: r'FGTS|Guia do FGTS|GFD|Fundo de Garantia',
        TipoDocumento.GPS: r'GPS|Guia.*Previd[eê]ncia Social|Previd[eê]ncia.*Social.*Guia',
        TipoDocumento.NFSE: r'\bNFS-e\b|Nota Fiscal de Serviços',
        TipoDocumento.NFE: r'\bNF-e\b|Nota Fiscal Eletrônica\b',
        TipoDocumento.NFCE: r'\bNFC-e\b|Nota Fiscal ao Consumidor\b',
        TipoDocumento.NF3E: r'\bNF3-e\b',
        TipoDocumento.CTE: r'\bCT-e\b|Conhecimento de Transporte\b',
        TipoDocumento.BOLETO: r'\bBoleto\b|Recibo do Pagador',
        TipoDocumento.FATURA: r'\bFatura\b|Nota de Pagamento\b'
    }
    for tipo, pattern in patterns.items():
        if re.search(pattern, texto, re.IGNORECASE):
            return tipo
    return TipoDocumento.DESCONHECIDO


def preprocess_text(text):
    text = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    return re.sub(r'\s+', ' ', text).strip()


def extract_text_with_trocr(image):
    processor = TrOCRProcessor.from_pretrained("microsoft/trocr-large-handwritten")
    model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-large-handwritten").to("cpu")

    # Pré-processa a imagem
    preprocessed_image = preprocess_image(image)

    # Garante que a imagem está em formato numpy array para o processor
    pixel_values = processor(images=np.array(preprocessed_image), return_tensors="pt").pixel_values
    generated_ids = model.generate(pixel_values, max_new_tokens=200)
    text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
    return text.strip()


def convert_pdf_to_images(pdf_path):
    """Converte PDF em imagens RGB e salva uma imagem de teste."""
    # Extrai as imagens do PDF
    images = convert_from_path(pdf_path, dpi=300, poppler_path=r'C:\poppler-24.08.0\Library\bin')

    # Verifica e cria diretório para salvar imagem de teste, se necessário
    test_image_path = "C:/PDF/notas/test_image.png"
    try:
        images[0].save(test_image_path, "PNG")
        print(f"Imagem de teste salva em: {test_image_path}")
    except Exception as e:
        print(f"Erro ao salvar imagem de teste: {e}")

    # Converte todas as imagens para RGB para garantir compatibilidade
    return [image.convert("RGB") for image in images]


def process_pdf_in_batches(pdf_path):
    images = convert_pdf_to_images(pdf_path)
    for i, image in enumerate(images):
        print(f"Processando página {i + 1}")
        text = extract_text_with_trocr(image)
        print(f"Texto extraído:\n{text}\n")


def convert_pdf_to_images(pdf_path):
    images = convert_from_path(pdf_path, dpi=300, poppler_path=r'C:\poppler-24.08.0\Library\bin')
    images[0].save("test_image.png", "PNG")  # Salva a primeira imagem como teste
    return images


def preprocess_image(image):
    """Garante que a imagem esteja no formato RGB."""
    if len(np.array(image).shape) == 2:  # Verifica se é uma imagem em escala de cinza
        image = cv2.cvtColor(np.array(image), cv2.COLOR_GRAY2RGB)
    elif image.mode != "RGB":
        image = image.convert("RGB")  # Converte para RGB se necessário
    return image


def processar_documento(pdf_path):
    images = convert_pdf_to_images(pdf_path)
    conteudo_total = ""

    for image in images:
        preprocessed_image = preprocess_image(image)
        texto = extract_text_with_trocr(preprocessed_image)
        conteudo_total += f"{texto}\n"

    tipo = identificar_tipo_documento(conteudo_total)
    documento = {"tipo_identificado": tipo, "conteudo": conteudo_total.strip()}

    json_data = json.dumps(documento, ensure_ascii=False, indent=4)
    print(json_data)


def main(pdf_path):
    if not os.path.exists(pdf_path):
        print(f"Erro: O arquivo '{pdf_path}' não foi encontrado.")
        sys.exit(1)

    processar_documento(pdf_path)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python extract_text_transformers.py <caminho_do_pdf>")
        sys.exit(1)

    pdf_path = sys.argv[1]
    main(pdf_path)
