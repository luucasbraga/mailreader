import fitz  # PyMuPDF
import easyocr
import cv2
import numpy as np

def pdf_to_images(pdf_path, dpi=300):
    doc = fitz.open(pdf_path)
    images = []
    for page_num in range(doc.page_count):
        pix = doc.load_page(page_num).get_pixmap(dpi=dpi)
        img = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)
        images.append(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY))
    doc.close()
    return images

def extract_text_from_images(images):
    reader = easyocr.Reader(['pt', 'en'], gpu=True)
    result = []
    for img in images:
        text = reader.readtext(img, detail=0)
        result.append(" ".join(text))
    return " ".join(result)

# Exemplo de uso
pdf_path = "C:/PDF/notas/1.pdf"
images = pdf_to_images(pdf_path)
texto = extract_text_from_images(images)
print(texto)
