import os
import re
import sys
import json
import unicodedata
from concurrent.futures import ThreadPoolExecutor, as_completed

import cv2
import easyocr
import fitz  # PyMuPDF
import numpy as np
from pdf2image import convert_from_path


class TipoDocumento:
    NFE = "NF-e"
    NFSE = "NFS-e"
    NFCE = "NFC-e"
    NF3E = "NF3-e"
    CTE = "CT-e"
    BOLETO = "Boleto"
    FATURA = "Fatura"
    DESCONHECIDO = "Desconhecido"


def identificar_tipo_documento(texto):
    patterns = {
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


def extract_text_from_pdf(pdf_path):
    try:
        doc = fitz.open(pdf_path)
        text = "".join([page.get_text() for page in doc])
        doc.close()
        return clean_text(text)
    except Exception as e:
        print(f"Erro ao ler PDF: {e}")
        return ""


def convert_pdf_to_images(pdf_path, dpi=300):
    """Converte PDF em imagens para uso com OCR."""
    try:
        return convert_from_path(pdf_path, dpi=dpi, poppler_path=r"C:\poppler-24.08.0\Library\bin")
    except Exception as e:
        print(f"Erro ao converter PDF para imagens: {e}")
        return []


def preprocess_image(image):
    gray = cv2.cvtColor(np.array(image), cv2.COLOR_BGR2GRAY)
    _, binary = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return binary


def extract_text_with_ocr(images):
    reader = easyocr.Reader(['pt', 'en'], gpu=True)
    results = []

    def process_image(img):
        processed_img = preprocess_image(img)
        return reader.readtext(processed_img, detail=0)

    with ThreadPoolExecutor() as executor:
        futures = [executor.submit(process_image, img) for img in images]
        for future in as_completed(futures):
            try:
                text = future.result()
                results.append(" ".join(text))
            except Exception as e:
                print(f"Erro ao processar imagem: {e}")

    return clean_text(" ".join(results))


def normalize_text(text):
    """Remove acentos e caracteres especiais."""
    nfkd_form = unicodedata.normalize('NFKD', text)
    normalized_text = "".join([c for c in nfkd_form if not unicodedata.combining(c)])
    return normalized_text


def clean_text(text):
    text = re.sub(r"\s{2,}", " ", text)  # Remove múltiplos espaços
    text = re.sub(r"R\$\s*\n\s*", "R$", text)  # Remove quebras de linha em valores monetários
    return normalize_text(text.strip())


def processar_documento(pdf_content):
    tipo = identificar_tipo_documento(pdf_content)
    documento = {"tipo": tipo, "conteudo": pdf_content}

    # Converter para JSON e garantir codificação UTF-8
    json_data = json.dumps(documento, ensure_ascii=False, indent=4)
    print(json_data.encode('utf-8').decode('utf-8'))


def main(pdf_path):
    texto = extract_text_from_pdf(pdf_path)

    if not texto:
        print("Texto vazio. Tentando OCR...")
        images = convert_pdf_to_images(pdf_path)
        if images:
            texto = extract_text_with_ocr(images)

    if texto:
        processar_documento(texto)
    else:
        print("Erro: Não foi possível extrair texto do PDF.")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python extract_text.py <caminho_do_pdf>")
        sys.exit(1)

    pdf_path = sys.argv[1]
    if not os.path.exists(pdf_path):
        print(f"Erro: O arquivo '{pdf_path}' não foi encontrado.")
        sys.exit(1)

    main(pdf_path)
